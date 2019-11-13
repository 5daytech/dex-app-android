package com.fridaytech.dex.presentation.orders

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.fridaytech.dex.R
import com.fridaytech.dex.core.ui.CoreFragment
import com.fridaytech.dex.presentation.common.TransactionSentDialog
import com.fridaytech.dex.presentation.exchangehistory.ExchangeHistoryActivity
import com.fridaytech.dex.presentation.main.IFocusListener
import com.fridaytech.dex.presentation.orders.info.OrderInfoDialog
import com.fridaytech.dex.presentation.orders.model.EOrderSide
import com.fridaytech.dex.presentation.orders.model.FillOrderInfo
import com.fridaytech.dex.presentation.widgets.MainToolbar
import com.fridaytech.dex.utils.ui.ToastHelper
import com.fridaytech.dex.utils.visible
import kotlinx.android.synthetic.main.fragment_orders_host.*

class OrdersHostFragment : CoreFragment(R.layout.fragment_orders_host),
    IFocusListener {

    private var adapter: OrdersHostAdapter? = null
    private lateinit var viewModel: OrdersViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager?.let {
            adapter =
                OrdersHostAdapter(it)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.let {
            viewModel = ViewModelProviders.of(it).get(OrdersViewModel::class.java)

            viewModel.availablePairs.observe(this, Observer { availablePairs ->
                orders_host_pair_picker?.refreshPairs(availablePairs)
            })

            viewModel.selectedPairPosition.observe(this, Observer { selectedPair ->
                orders_host_pair_picker?.selectedPair = selectedPair
            })

            viewModel.orderInfoEvent.observe(this, Observer {
                OrderInfoDialog.show(childFragmentManager, it)
            })

            viewModel.fillOrderEvent.observe(this, Observer { fillInfo ->
                (context as? OrderFillListener)?.requestFill(fillInfo)
            })

            viewModel.exchangeCoinSymbol.observe(this, Observer {
                if (orders_tab_layout.tabCount > 0) {
                    val firstTab = orders_tab_layout.getTabAt(0)
                    firstTab?.text = "SELL $it"
                    val secondTab = orders_tab_layout.getTabAt(1)
                    secondTab?.text = "BUY $it"
                }
            })

            viewModel.messageEvent.observe(this, Observer {
                ToastHelper.showSuccessMessage(it)
            })

            viewModel.errorEvent.observe(this, Observer {
                ToastHelper.showErrorMessage(it)
            })

            viewModel.cancelAllConfirmEvent.observe(this, Observer { cancelInfo ->
                fragmentManager?.let {
                    CancelOrderConfirmDialog.show(
                        it,
                        cancelInfo
                    )
                }
            })

            viewModel.transactionSentEvent.observe(this, Observer { transactionHash ->
                fragmentManager?.let { TransactionSentDialog.show(it, transactionHash) }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orders_view_pager?.adapter = adapter
        orders_tab_layout?.setupWithViewPager(orders_view_pager)

        orders_host_pair_picker?.init {
            viewModel.onPickPair(it)
        }

        toolbar?.bind(rightActionButton = MainToolbar.ActionInfo(
            R.drawable.ic_exchange_history,
            R.string.title_trade_history
        ) { activity?.let { ExchangeHistoryActivity.start(it) } })
    }

    override fun onFocused() {
        coordinator?.visible = true
    }

    companion object {
        fun newInstance() = OrdersHostFragment()
    }

    interface OrderFillListener {
        fun requestFill(fillInfo: FillOrderInfo)
    }

    private class OrdersHostAdapter(
        fm: FragmentManager
    ) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> OrdersFragment.newInstance(EOrderSide.BUY)
                1 -> OrdersFragment.newInstance(EOrderSide.SELL)
                2 -> OrdersFragment.newInstance(EOrderSide.MY)
                else -> throw Exception("Orders host adapter fragment at position not exist $position")
            }
        }

        override fun getCount(): Int = 3

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                2 -> "My Orders"
                else -> ""
            }
        }
    }
}
