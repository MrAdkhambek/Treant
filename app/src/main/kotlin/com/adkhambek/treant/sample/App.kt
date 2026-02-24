package com.adkhambek.treant.sample

import com.adkhambek.treant.CommonsLog
import com.adkhambek.treant.Log
import com.adkhambek.treant.Log4j
import com.adkhambek.treant.Log4j2
import com.adkhambek.treant.Slf4j
import com.adkhambek.treant.XSlf4j

@Slf4j
class MyService {
    fun doWork() {
        log.info("MyService is doing work")
    }
}

@Log
class AnotherService {

    companion object {
        const val NAME = "AnotherService"
    }

    fun process() {
        log.info("Processing in $NAME")
    }
}

@CommonsLog
class CommonsService {
    fun process() {
        log.info("CommonsService processing")
    }
}

@Log4j
class Log4jService {
    fun process() {
        log.info("Log4jService processing")
    }
}

@Log4j2
class Log4j2Service {
    fun process() {
        log.info("Log4j2Service processing")
    }
}

@XSlf4j
class XSlf4jService {
    fun process() {
        log.info("XSlf4jService processing")
    }
}

fun main() {
    MyService().doWork()
    AnotherService().process()
    CommonsService().process()
    Log4jService().process()
    Log4j2Service().process()
    XSlf4jService().process()
}
