package org.bura.beanschema;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;


/**
 * Class annotation used to assist in the creation of bean schema in classes. The {@code @Schema} annotation instructs
 * the compiler to execute an AST transformation which adds the static descriptors in classes.
 * <p/>
 * It allows you to simple access for bean schema (field names) and protects from exceptions in runtime.
 * 
 * <pre>
 * {@code @Schema}
 * class User {
 *     String id
 *     String name
 *     int age
 * }
 * 
 * println User._id
 * println User._name
 * println User._birthdate // Compile time exception
 * </pre>
 * 
 * Which will have this output:
 * 
 * <pre>
 * id
 * name
 * groovy.lang.MissingPropertyException: No such property: _birthdate for class: User
 * </pre>
 * 
 * @author Andrey Bloschetsov
 */
@java.lang.annotation.Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE})
@GroovyASTTransformationClass("org.bura.beanschema.SchemaASTTransformation")
public @interface Schema {}
