package org.lst.trading.lib.util

import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import rx.Observable
import rx.functions.Func1
import rx.schedulers.Schedulers
import java.io.IOException
import java.util.function.Consumer

object Http {
    private val log = LoggerFactory.getLogger(Http::class.java)
    private var client: CloseableHttpClient? = null

    @get:Synchronized
    val defaultHttpClient: HttpClient?
        get() {
            if (client == null) {
                client = HttpClients.createDefault()
            }
            return client
        }

    @JvmOverloads
    operator fun get(
        url: String?,
        configureRequest: Consumer<HttpGet?> = Consumer { x: HttpGet? -> }
    ): Observable<HttpResponse> {
        val request = HttpGet(url)
        configureRequest.accept(request)
        return Observable.create<HttpResponse> { s ->
            try {
                log.debug("GET {}", url)
                s.onNext(defaultHttpClient!!.execute(request))
                s.onCompleted()
            } catch (e: IOException) {
                s.onError(e)
            }
        }.subscribeOn(Schedulers.io())
    }

    fun asString(): Func1<in HttpResponse, out Observable<String?>> {
        return Func1 { t: HttpResponse ->
            try {
                return@Func1 Observable.just<String?>(EntityUtils.toString(t.entity))
            } catch (e: IOException) {
                return@Func1 Observable.error<String?>(e)
            }
        }
    }
}
