package io.codera.quant.config

import com.google.inject.Guice
import org.junit.runners.BlockJUnit4ClassRunner

class GuiceJUnit4Runner(klass: Class<*>?) : BlockJUnit4ClassRunner(klass) {
    @Throws(Exception::class)
    public override fun createTest(): Any {
        val obj = super.createTest()
        Guice.createInjector(TestConfig()).injectMembers(obj)
        return obj
    }
}
