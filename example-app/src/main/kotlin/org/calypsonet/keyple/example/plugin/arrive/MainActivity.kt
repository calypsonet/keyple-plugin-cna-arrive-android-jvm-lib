/* **************************************************************************************
 * Copyright (c) 2026 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.example.plugin.arrive

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.calypsonet.keyple.example.plugin.arrive.MessageDisplayAdapter.Message
import org.calypsonet.keyple.example.plugin.arrive.MessageDisplayAdapter.MessageType
import org.calypsonet.keyple.example.plugin.arrive.databinding.ActivityMainBinding
import org.calypsonet.keyple.plugin.arrive.ArriveConstants
import org.calypsonet.keyple.plugin.arrive.ArriveContactlessProtocols
import org.calypsonet.keyple.plugin.arrive.ArrivePluginFactoryProvider
import org.eclipse.keyple.card.calypso.CalypsoExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamExtensionService
import org.eclipse.keyple.card.calypso.crypto.legacysam.LegacySamUtil
import org.eclipse.keyple.core.service.*
import org.eclipse.keyple.core.util.HexUtil
import org.eclipse.keypop.calypso.card.WriteAccessLevel.DEBIT
import org.eclipse.keypop.calypso.card.WriteAccessLevel.LOAD
import org.eclipse.keypop.calypso.card.WriteAccessLevel.PERSONALIZATION
import org.eclipse.keypop.calypso.card.card.CalypsoCard
import org.eclipse.keypop.calypso.card.transaction.SecureRegularModeTransactionManager
import org.eclipse.keypop.calypso.card.transaction.SymmetricCryptoSecuritySetting
import org.eclipse.keypop.calypso.crypto.legacysam.sam.LegacySam
import org.eclipse.keypop.reader.*
import org.eclipse.keypop.reader.ObservableCardReader.DetectionMode.REPEATING
import org.eclipse.keypop.reader.ObservableCardReader.NotificationMode.ALWAYS
import org.eclipse.keypop.reader.selection.CardSelectionManager
import org.eclipse.keypop.reader.spi.CardReaderObservationExceptionHandlerSpi
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi
import timber.log.Timber

class MainActivity :
    AppCompatActivity(), CardReaderObserverSpi, CardReaderObservationExceptionHandlerSpi {

  private val cardSelectionManager =
      SmartCardServiceProvider.getService().readerApiFactory.createCardSelectionManager()
  private lateinit var securitySettings: SymmetricCryptoSecuritySetting

  private lateinit var cardReader: ObservableCardReader
  private lateinit var samReader: CardReader
  private var isInitializationFinalized = false

  private lateinit var binding: ActivityMainBinding
  private lateinit var messageDisplayAdapter: RecyclerView.Adapter<*>
  private val messages = arrayListOf<Message>()

  companion object {
    const val ISO_14443_4_LOGICAL_PROTOCOL = "ISO_14443_4"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initUI()
    lifecycleScope.launch(Dispatchers.IO) { initTransaction() }
  }

  override fun onResume() {
    super.onResume()
    if (isInitializationFinalized) {
      startCardDetection()
    }
  }

  override fun onPause() {
    if (isInitializationFinalized) {
      stopCardDetection()
    }
    super.onPause()
  }

  override fun onDestroy() {
    SmartCardServiceProvider.getService()?.plugins?.forEach {
      SmartCardServiceProvider.getService()?.unregisterPlugin(it.name)
    }
    super.onDestroy()
  }

  private fun initUI() {
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)
    supportActionBar?.title = getString(R.string.app_name)
    supportActionBar?.subtitle = "Arrive Plugin"

    messageDisplayAdapter = MessageDisplayAdapter(messages)
    binding.messageRecyclerView.layoutManager = LinearLayoutManager(this)
    binding.messageRecyclerView.adapter = messageDisplayAdapter

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  }

  private fun addMessage(type: MessageType, message: String) {
    lifecycleScope.launch(Dispatchers.Main) {
      messages.add(Message(type, message))
      messageDisplayAdapter.notifyItemInserted(messages.lastIndex)
      binding.messageRecyclerView.smoothScrollToPosition(messages.size - 1)
    }
  }

  private fun showAlertDialogWithAction(
      titleRes: String,
      messageRes: String,
      onOkClick: () -> Unit
  ) {
    lifecycleScope.launch(Dispatchers.Main) {
      AlertDialog.Builder(this@MainActivity)
          .setTitle(titleRes)
          .setMessage(messageRes)
          .setPositiveButton("OK") { _, _ -> onOkClick() }
          .setCancelable(false)
          .show()
    }
  }

  private suspend fun initTransaction() {
    try {
      initReaders()
      initSecuritySettings()
      prepareCardSelection()
      startCardDetection()
      isInitializationFinalized = true
    } catch (e: Exception) {
      Timber.e(e, "Failed to initialize transaction")
      showAlertDialogWithAction(
          "Initialization Error",
          "Unable to initialize the application\n\nThe application will now close") {
            finishAffinity()
          }
    }
  }

  private suspend fun initReaders() {
    Timber.i("Initializing readers")

    // register plugin
    val arrivePlugin =
        SmartCardServiceProvider.getService()
            .registerPlugin(ArrivePluginFactoryProvider.provideFactory(context = this@MainActivity))

    // init card reader
    cardReader = arrivePlugin.getReader(ArriveConstants.CARD_READER_NAME) as ObservableCardReader

    cardReader.setReaderObservationExceptionHandler(this)
    cardReader.addObserver(this)

    (cardReader as ConfigurableCardReader).activateProtocol(
        ArriveContactlessProtocols.ISO_14443_4_AB.name, ISO_14443_4_LOGICAL_PROTOCOL)

    // init SAM reader
    samReader = arrivePlugin.getReader(ArriveConstants.SAM.SAM_1.readerName)

    Timber.i("Readers initialized")
  }

  private fun initSecuritySettings() {
    Timber.i("Initializing security settings")

    val samSelectionManager: CardSelectionManager =
        SmartCardServiceProvider.getService().readerApiFactory.createCardSelectionManager()

    samSelectionManager.prepareSelection(
        SmartCardServiceProvider.getService()
            .readerApiFactory
            .createBasicCardSelector()
            .filterByPowerOnData(
                LegacySamUtil.buildPowerOnDataFilter(LegacySam.ProductType.SAM_C1, null)),
        LegacySamExtensionService.getInstance()
            .legacySamApiFactory
            .createLegacySamSelectionExtension())

    try {
      val samSelectionResult = samSelectionManager.processCardSelectionScenario(samReader)

      check(samSelectionResult.activeSmartCard != null) { "No SAM found" }

      securitySettings =
          CalypsoExtensionService.getInstance()
              .calypsoCardApiFactory
              .createSymmetricCryptoSecuritySetting(
                  LegacySamExtensionService.getInstance()
                      .legacySamApiFactory
                      .createSymmetricCryptoCardTransactionManagerFactory(
                          samReader, samSelectionResult.activeSmartCard!! as LegacySam))
              .assignDefaultKif(PERSONALIZATION, 0x21) // required for old Innovatron B Prime cards
              .assignDefaultKif(LOAD, 0x27)
              .assignDefaultKif(DEBIT, 0x30)

      Timber.i("Security settings initialized")
    } catch (e: Exception) {
      Timber.e(e, "Failed to initialize security settings")
      showAlertDialogWithAction(
          "SAM Error",
          "Unable to communicate with the SAM\n\nThe application will now close",
          onOkClick = { finishAffinity() })
    }
  }

  private fun prepareCardSelection() {
    Timber.i("Preparing card selection")
    cardSelectionManager.prepareSelection(
        SmartCardServiceProvider.getService()
            .readerApiFactory
            .createIsoCardSelector()
            .filterByCardProtocol(ISO_14443_4_LOGICAL_PROTOCOL)
            .filterByDfName(CalypsoConstants.AID),
        CalypsoExtensionService.getInstance()
            .calypsoCardApiFactory
            .createCalypsoCardSelectionExtension())
    cardSelectionManager.scheduleCardSelectionScenario(cardReader, ALWAYS)
    Timber.i("Card selection prepared")
  }

  private fun startCardDetection() {
    Timber.i("Starting card detection")
    cardReader.startCardDetection(REPEATING)
    addMessage(
        MessageType.ACTION,
        "Waiting for card presentation...\n" +
            "\nAcceptable cards:" +
            "\n- Calypso (AID: ${CalypsoConstants.AID})")
    Timber.i("Card detection started")
  }

  private fun stopCardDetection() {
    Timber.i("Stopping card detection")
    cardReader.stopCardDetection()
    addMessage(MessageType.ACTION, "Card detection stopped")
    Timber.i("Card detection stopped")
  }

  override fun onReaderEvent(readerEvent: CardReaderEvent) {
    lifecycleScope.launch(Dispatchers.IO) {
      when (readerEvent.type) {
        CardReaderEvent.Type.CARD_MATCHED -> handleCardMatchedEvent(readerEvent)
        CardReaderEvent.Type.CARD_INSERTED -> handleCardInsertedEvent()
        CardReaderEvent.Type.CARD_REMOVED -> handleCardRemovedEvent()
        else -> {
          // Do nothing
        }
      }
    }
  }

  override fun onReaderObservationError(pluginName: String, readerName: String, e: Throwable) {
    addMessage(MessageType.EVENT, "Reader observation error: ${e.message}")
    Timber.e(e, "Failed to observe reader")
  }

  private fun handleCardMatchedEvent(cardReaderEvent: CardReaderEvent) {
    try {
      val selectionsResult =
          cardSelectionManager.parseScheduledCardSelectionsResponse(
              cardReaderEvent.scheduledCardSelectionsResponse)
      when (val card = selectionsResult.activeSmartCard) {
        is CalypsoCard -> {
          handleCalypsoCard(card)
        }
        else -> {
          addMessage(MessageType.RESULT, "Unknown card type")
        }
      }
    } catch (e: Exception) {
      Timber.e(e)
      addMessage(MessageType.RESULT, "Exception: ${e.message}")
    } finally {
      cardReader.finalizeCardProcessing()
      addMessage(MessageType.ACTION, "Waiting for card removal...")
    }
  }

  private fun handleCalypsoCard(calypsoCard: CalypsoCard) {

    val cardTransactionManager =
        CalypsoExtensionService.getInstance()
            .calypsoCardApiFactory
            .createSecureRegularModeTransactionManager(cardReader, calypsoCard, securitySettings)

    val duration = measureTimeMillis {
      (cardTransactionManager as SecureRegularModeTransactionManager)
          .prepareOpenSecureSession(LOAD)
          .prepareReadRecords(
              CalypsoConstants.SFI_ENV_HOLDER,
              CalypsoConstants.REC_1,
              CalypsoConstants.REC_1,
              CalypsoConstants.REC_SIZE)
          .prepareReadRecords(
              CalypsoConstants.SFI_EVENT_LOG,
              CalypsoConstants.REC_1,
              CalypsoConstants.REC_1,
              CalypsoConstants.REC_SIZE)
          .prepareCloseSecureSession()
          .processCommands(ChannelControl.CLOSE_AFTER)
    }

    val efEnvironmentHolder =
        HexUtil.toHex(calypsoCard.getFileBySfi(CalypsoConstants.SFI_ENV_HOLDER).data.content)

    val eventLog =
        HexUtil.toHex(calypsoCard.getFileBySfi(CalypsoConstants.SFI_EVENT_LOG).data.content)

    // Delayed display of messages preceding the transaction in order to optimize processing time
    addMessage(MessageType.EVENT, "Card matched")
    addMessage(MessageType.RESULT, "Calypso DF name:\n${HexUtil.toHex(calypsoCard.dfName)}")
    addMessage(MessageType.ACTION, "Starting secure transaction")
    addMessage(
        MessageType.RESULT,
        "EnvironmentHolder file:\n$efEnvironmentHolder\n\nEventLog file:\n$eventLog")
    addMessage(MessageType.ACTION, "Transaction duration: $duration ms")
  }

  private fun handleCardInsertedEvent() {
    addMessage(
        MessageType.EVENT,
        "Unrecognized card: ${(cardReader as ConfigurableCardReader).currentProtocol}")
    cardReader.finalizeCardProcessing()
    addMessage(MessageType.ACTION, "Waiting for card removal...")
  }

  private fun handleCardRemovedEvent() {
    addMessage(MessageType.EVENT, "Card removed")
    addMessage(
        MessageType.ACTION,
        "Waiting for card presentation...\n" +
            "\nAcceptable cards:" +
            "\n- Calypso (AID: ${CalypsoConstants.AID})")
  }
}
