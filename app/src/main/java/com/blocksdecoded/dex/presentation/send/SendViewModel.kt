package com.blocksdecoded.dex.presentation.send

import androidx.lifecycle.MutableLiveData
import com.blocksdecoded.dex.App
import com.blocksdecoded.dex.R
import com.blocksdecoded.dex.core.adapter.FeeRatePriority
import com.blocksdecoded.dex.core.adapter.IAdapter
import com.blocksdecoded.dex.core.adapter.SendStateError
import com.blocksdecoded.dex.core.model.Coin
import com.blocksdecoded.dex.utils.uiObserver
import com.blocksdecoded.dex.core.ui.CoreViewModel
import com.blocksdecoded.dex.core.ui.SingleLiveEvent
import com.blocksdecoded.dex.utils.Logger
import com.blocksdecoded.dex.core.manager.clipboard.ClipboardManager
import com.blocksdecoded.dex.presentation.send.model.ReceiveAddressInfo
import com.blocksdecoded.dex.presentation.send.model.SendInfo
import com.blocksdecoded.dex.presentation.send.model.SendUserInput
import java.lang.Exception
import java.math.BigDecimal

class SendViewModel: CoreViewModel() {
    private val ratesConverter = App.ratesConverter
    private lateinit var adapter: IAdapter
    private var userInput = SendUserInput()

    var decimalSize: Int? = null

    val coin = MutableLiveData<Coin>()
    val receiveAddress = MutableLiveData<ReceiveAddressInfo>()
    val sendEnabled = MutableLiveData<Boolean>()
    val amount = MutableLiveData<BigDecimal>()
    val sendInfo = MutableLiveData<SendInfo>()

    val confirmEvent = SingleLiveEvent<SendConfirmDialog.SendConfirmData>()
    val dismissEvent = SingleLiveEvent<Unit>()
    val dismissWithSuccessEvent = SingleLiveEvent<Unit>()
    val openBarcodeScannerEvent = SingleLiveEvent<Unit>()

    fun init(coinCode: String) {
        val adapter = App.adapterManager.adapters
                .firstOrNull { it.coin.code == coinCode }

        if (adapter == null) {
            dismissEvent.call()
            return
        } else {
            this.adapter = adapter
        }
    
        coin.value = adapter.coin
        decimalSize = adapter.decimal
        reset()
    }

    private fun reset() {
        userInput = SendUserInput()
        sendEnabled.value = false
        amount.value = userInput.amount
        receiveAddress.value = ReceiveAddressInfo("", 0)
        sendInfo.value = SendInfo(BigDecimal.ZERO, false)
    }

    private fun confirm() {
        val fee = adapter.fee(userInput.amount, null, feePriority = FeeRatePriority.MEDIUM)
        val feeFiatAmount = ratesConverter.getCoinsPrice("ETH", fee)
        val fiatAmount = sendInfo.value?.fiatAmount ?: BigDecimal.ZERO

        val sendConfirmData = SendConfirmDialog.SendConfirmData(
            adapter.coin,
            userInput.address ?: "",
            userInput.amount,
            fiatAmount,
            fee,
            fiatAmount + feeFiatAmount
        ) {
            send(userInput)
        }

        confirmEvent.postValue(sendConfirmData)
    }

    private fun send(userInput: SendUserInput) {
        val address = userInput.address ?: return

        val amount = userInput.amount
        if (amount == BigDecimal.ZERO) return

        adapter.send(address, amount, userInput.feePriority)
            .uiObserver()
            .subscribe({
                dismissWithSuccessEvent.call()
            }, {
                Logger.e(it)
                messageEvent.postValue(R.string.error_send)
            }).let { disposables.add(it) }
    }
    
    private fun refreshSendEnable() {
        val validAmount = userInput.amount > BigDecimal.ZERO &&
                !(sendInfo.value?.error ?: false)

        val validAddress = userInput.address != null &&
                (receiveAddress.value?.error ?: 1) == 0

        sendEnabled.value = validAmount && validAddress
    }

    private fun refreshSendAmount(sendAmount: BigDecimal) {
        val info = SendInfo(
            ratesConverter.getCoinsPrice(adapter.coin.code, sendAmount),
            false
        )

        adapter.validate(sendAmount, null, FeeRatePriority.MEDIUM).forEach {
            when(it) {
                is SendStateError.InsufficientAmount -> {
                    info.error = true
                }
                is SendStateError.InsufficientFeeBalance -> {
                    info.error = true
                }
            }
        }

        sendInfo.value = info
    }

    private fun setAddress(address: String?) {
        userInput.address = address
        val error = try {
            adapter.validate(address ?: "")
            0
        } catch (e: Exception) {
            R.string.send_invalid_recipient_address
        }

        receiveAddress.value = ReceiveAddressInfo(userInput.address, error)

        refreshSendEnable()
    }

    fun onAmountChanged(amount: BigDecimal) {
        if (userInput.amount != amount) {
            userInput.amount = amount

            refreshSendAmount(amount)

            refreshSendEnable()
        }
    }

    fun onBarcodeClick() {
        openBarcodeScannerEvent.call()
    }

    fun onScanResult(contents: String?) {
        if (contents != null && contents.isNotEmpty()) {
            setAddress(contents)
        }
    }

    fun onMaxClicked() {
        val balance = adapter.availableBalance(adapter.receiveAddress, FeeRatePriority.HIGHEST)
        onAmountChanged(balance)
        amount.value = balance
    }

    fun onSwitchClick() {

    }

    fun onPasteClick() {
        setAddress(ClipboardManager.getCopiedText())
    }

    fun onDeleteAddressClick() {
        receiveAddress.value = ReceiveAddressInfo("", 0)
        userInput.address = null
	    refreshSendEnable()
    }

    fun onSendClicked() {
        confirm()
    }
}