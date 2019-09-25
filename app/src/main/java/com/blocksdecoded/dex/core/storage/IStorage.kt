package com.blocksdecoded.dex.core.storage

import com.blocksdecoded.dex.core.model.EnabledCoin
import com.blocksdecoded.dex.core.model.Market
import com.blocksdecoded.dex.core.model.Rate
import io.reactivex.Flowable
import io.reactivex.Single

interface IMarketsStorage {
    fun getAllMarkets(): Single<List<Market>>

    fun getMarket(coinCode: String): Single<Market>

    fun save(vararg markets: Market)

    fun deleteAll()
}

interface IRatesStorage {
    fun getRateSingle(coinCode: String, timeStamp: Long): Single<Rate>

    fun getRate(coinCode: String, timeStamp: Long): Rate?

    fun save(vararg rates: Rate)

    fun deleteAll()
}

interface IEnabledCoinsStorage {
    fun enabledCoinsObservable(): Flowable<List<EnabledCoin>>

    fun save(coins: List<EnabledCoin>)

    fun deleteAll()
}