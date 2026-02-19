package com.adkhambek.treant.sample

import com.adkhambek.treant.Slf4j

@Slf4j
class MyService {
    fun doWork() {
        logger.info("MyService is doing work")
    }
}

@Slf4j
class AnotherService {
    companion object {
        const val NAME = "AnotherService"
    }

    fun process() {
        logger.debug("Processing in $NAME")
    }
}

fun main() {
    val service = MyService()
    service.doWork()

    val another = AnotherService()
    another.process()
}
