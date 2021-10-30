package org.pfj.lang

import spock.lang.Specification

import java.util.function.Consumer

import static org.pfj.lang.Causes.cause
import static org.pfj.lang.Causes.with1
import static org.pfj.lang.Result.failure
import static org.pfj.lang.Result.success

/**
 * @project pragmatica
 * @author Xiao Luo
 * @created 2021-10-28
 */
class ResultSpec extends Specification {
    def "success results are equal when their value equal"() {
        expect:
          success('123') == success(123).map(Objects::toString)
    }

    def "failure results are equal when their causes equal"() {
        expect:
          failure(cause('123')) == success(123).filter(with1('{0}'), v -> v < 0)
          failure(cause('321')) != success(123).filter(with1('{0}'), v -> v < 0)
    }

    def "success result can be transformed with map"() {
        expect:
          success(input).map(Objects::toString)
                        .onFailure(Cause::message)
                        .onSuccess(value::equals)
        where:
          value    | input
          '123'    | 123
          'java'   | "java"
          '[0, 1]' | [0, 1]
    }

    def "success result can be transformed with flatMap"(String value) {
        expect:
          success(input).flatMap(Objects::toString >> Result::success)
                        .onFailure(Cause::message)
                        .onSuccess(value::equals)
        where:
          value    | input
          '123'    | 123
          'java'   | "java"
          '[0, 1]' | [0, 1]
    }

    def "failure result remains unchanged after map"(String errMsg) {
        expect:
          failure(cause(errMsg)).map(Objects::toString)
                                .onFailure(Cause::message >> errMsg::equals)
                                .onSuccess(Functions.blackHole())
        where:
          errMsg             | _
          'some error'       | _
          'some other error' | _
    }

    def "failure result remains unchanged after flatMap"() {
        expect:
          failure(cause(errMsg)).flatMap(Objects::toString >> Result::success)
                                .onSuccess(Functions.blackHole())
                                .onFailure(Cause::message >> errMsg::equals)
        where:
          errMsg             | _
          'some error'       | _
          'some other error' | _
    }

    def "only one method is invoked on apply"() {
        Consumer failConsumer = Mock()
        Consumer successConsumer = Mock()
        when:
          success(123).apply(failConsumer, successConsumer)
        then:
          1 * successConsumer.accept(_)
          0 * successConsumer.accept(_)
        when:
          failure(cause('error')).apply(failConsumer, successConsumer)
        then:
          1 * failConsumer.accept(_)
          0 * successConsumer.accept(_)
    }

    def "onSuccess is invoked for success result"() {
        Consumer failureConsumer = Mock()
        Consumer successConsumer = Mock()
        when:
          success(123).onFailure(failureConsumer)
                      .onSuccess(successConsumer)
        then:
          1 * successConsumer.accept(_)
          0 * failureConsumer.accept(_)
    }

    def "onSuccessDo is invoked for success result"() {
        Runnable successRunnable = Mock()
        Runnable failureRunnable = Mock()
        when:
          success(123).onFailureDo(failureRunnable)
                      .onSuccessDo(successRunnable)
        then:
          1 * successRunnable.run()
          0 * failureRunnable.run()
    }

    def "onFailure is invoked for failure result"() {
        Consumer failureConsumer = Mock()
        Consumer successConsumer = Mock()
        when:
          failure(cause("123")).onFailure(failureConsumer)
                               .onSuccess(successConsumer)
        then:
          0 * successConsumer.accept(_)
          1 * failureConsumer.accept(_)
    }

    def "onFailureDo is invoked for failure result"() {
        Runnable successRunnable = Mock()
        Runnable failureRunnable = Mock()
        when:
          failure(cause("123")).onFailureDo(failureRunnable)
                               .onSuccessDo(successRunnable)
        then:
          0 * successRunnable.run()
          1 * failureRunnable.run()
    }

    def "results can be converted to option"() {
        expect:
          123 == success(123).toOption().or(0)
          failure(cause("321")).toOption().empty
    }

    def "result status can be checked"() {
        expect:
          success(123).success
          !success(123).failure
          failure(cause("321")).failure
          !failure(cause("321")).success
    }

    def "success result can be filtered"() {
        expect:
          success(123).filter(with1("Value {0} is below threshold"), value -> value > 123)
                      .onFailure(Cause::message >> 'Value 123 is below threshold'::equals)
    }

    def "lift wraps code which can throw exceptions"() {
        expect:
          Result.Failure.lift(Causes::fromThrowable, () -> throwingFunction(3))
                .onFailure(Cause::message >> 'Just throw exception 3'::equals)
    }

    def static throwingFunction(int i) {
        3 == i ?: { throw new IllegalStateException("Just throw exception $i") }
    }
}
