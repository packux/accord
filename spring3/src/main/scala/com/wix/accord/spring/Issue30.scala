package com.wix.accord.spring

import javax.annotation.Resource

import org.springframework.context.annotation.{AnnotationConfigApplicationContext, Bean, Configuration}
import org.springframework.validation.{BeanPropertyBindingResult, Validator}

import scala.beans.BeanProperty

/**
 * Created by tomer on 3/3/15.
 */

case class Test1( field: Int )
case object Test1 {
  import com.wix.accord.dsl._
  implicit val validatorOfTest1 = validator[ Test1 ] { t => t.field should be > 5 }
}

@Configuration class SpringContext {
  @Bean def accordValidatorResolver: AccordValidatorResolver = new CompanionObjectAccordValidatorResolver
  @Bean def validator = new AccordEnabledLocalValidationFactory
  @Bean def test2 = new Test2
}

class Test2 {
  @BeanProperty @Resource var validator: Validator = _

  def m(): Unit = {
    val t = Test1(3)
    val errors = new BeanPropertyBindingResult(t, t.getClass.getSimpleName)
    validator.validate(t, errors)
    println(errors)
  }
}

object Issue30 extends App {
  val ctx = new AnnotationConfigApplicationContext(classOf[SpringContext])
  val t2 = ctx.getBean(classOf[Test2])
  t2.m()

  /*
@ContextConfiguration( classes = Array( classOf[ SpringValidationConfiguration ] ) )
class AccordEnabledLocalValidationFactoryTest extends WordSpec with Matchers {

  new TestContextManager( this.getClass ).prepareTestInstance( this )

   */
}
