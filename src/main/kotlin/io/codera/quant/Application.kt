package io.codera.quant

import com.google.common.base.Preconditions
import com.google.inject.Guice
import io.codera.quant.config.Config
import io.codera.quant.strategy.Strategy
import io.codera.quant.strategy.StrategyRunner
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Entry point for PT trading app.
 */
object Application {
    private val logger = LoggerFactory.getLogger(Application::class.java)
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Starting app...")
        try {
            val cmd = getCmd(args)
            val injector = Guice.createInjector(
                Config(
                    cmd.getOptionValue("h"),
                    cmd.getOptionValue("p").toInt(),
                    cmd.getOptionValue("l")
                )
            )
            val strategyRunner = injector.getInstance(StrategyRunner::class.java)
            val strategy = injector.getInstance(Strategy::class.java)
            val symbolList = cmd.getOptionValue("l")
                .split(",".toRegex())
                .dropLastWhile(String::isEmpty)
                .toTypedArray()
            strategyRunner.run(strategy, listOf(*symbolList))
        } catch (e: Exception) {
            // oops, something went wrong
            logger.error("Something went wrong", e)
        }
    }

    private fun getCmd(args: Array<String>): CommandLine {
        // parse the command line arguments
        val options = Options()
        options.addOption("h", true, "Interactive Brokers host")
        options.addOption("p", true, "Interactive Brokers port")
        options.addOption("l", true, "List of symbols to trade")
        // create the parser
        val parser: CommandLineParser = DefaultParser()
        val cmd = parser.parse(options, args)
        require(cmd.getOptionValue("h") != null) {"host can not be null"}
        require(cmd.getOptionValue("p") != null) {"port can not be null"}
        require(cmd.getOptionValue("l") != null) {"symbol can not be null"}
        return cmd
    }
}
