package org.bura.beanschema;


import static org.codehaus.groovy.transform.AbstractASTTransformUtil.declStatement;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;


/**
 * Handles generation of code for the {@link Schema} annotation.
 * 
 * @author Andrey Bloschetsov
 */
@GroovyASTTransformation
public class SchemaASTTransformation extends AbstractASTTransformation {

	static final Class<Schema> MY_CLASS = Schema.class;

	static final ClassNode MY_TYPE = ClassHelper.make(MY_CLASS);

	static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();

	private static final ClassNode STRINGBUILDER_TYPE = ClassHelper.make(StringBuilder.class);

	private static final String SUBPATH_PARAM_NAME = "subpath";

	private static final List<ClassNode> simpleTypes = Arrays.asList(ClassHelper.int_TYPE, ClassHelper.Integer_TYPE,
			ClassHelper.long_TYPE, ClassHelper.Long_TYPE, ClassHelper.byte_TYPE, ClassHelper.Byte_TYPE,
			ClassHelper.short_TYPE, ClassHelper.Short_TYPE, ClassHelper.char_TYPE, ClassHelper.Character_TYPE,
			ClassHelper.float_TYPE, ClassHelper.Float_TYPE, ClassHelper.double_TYPE, ClassHelper.Double_TYPE,
			ClassHelper.boolean_TYPE, ClassHelper.Boolean_TYPE, ClassHelper.STRING_TYPE, ClassHelper.BigDecimal_TYPE,
			ClassHelper.BigInteger_TYPE, ClassHelper.Number_TYPE, ClassHelper.CLASS_Type, ClassHelper.Enum_Type,
			ClassHelper.make(Date.class));

	private static final AnnotationNode TRANSIENT_AN = new AnnotationNode(new ClassNode(Transient.class));

	@Override
	public void visit(ASTNode[] nodes, SourceUnit source) {
		init(nodes, source);
		AnnotatedNode parent = (AnnotatedNode) nodes[1];
		AnnotationNode anno = (AnnotationNode) nodes[0];
		if (!MY_TYPE.equals(anno.getClassNode()))
			return;

		if (parent instanceof ClassNode) {
			ClassNode cNode = (ClassNode) parent;
			checkNotInterface(cNode, MY_TYPE_NAME);

			createSchema(cNode);
		}
	}

	private void createSchema(ClassNode cNode) {
		List<FieldNode> addFields = new ArrayList<>();
		List<MethodNode> addMethods = new ArrayList<>();
		for (FieldNode field : cNode.getFields()) {
			if (!field.isFinal() && !field.isStatic()) {
				if (field.getAnnotations() != null && field.getAnnotations().isEmpty()
						&& !field.getAnnotations().contains(TRANSIENT_AN)) {

					String schemaName = "_" + field.getName();
					// check exists field schema descriptor
					for (FieldNode fieldIt : cNode.getFields()) {
						if (fieldIt.isStatic() && schemaName.equals(fieldIt.getName())) {
							addError(
									"Error during " + MY_TYPE_NAME + " processing: schema for field '"
											+ field.getName() + "' already exests.", field);
						}
					}

					// create field schema descriptor
					ConstantExpression schemaFieldValue = new ConstantExpression(field.getName());
					FieldNode schemaStaticField = new FieldNode(schemaName, FieldNode.ACC_PUBLIC | FieldNode.ACC_STATIC
							| FieldNode.ACC_FINAL, ClassHelper.STRING_TYPE, cNode, schemaFieldValue);
					addFields.add(schemaStaticField);

					if (!simpleTypes.contains(field.getType())) { // for composite type
						// check exists method schema descriptor
						for (MethodNode methodIt : cNode.getMethods(schemaName)) {
							if (methodIt.isStatic() && schemaName.equals(methodIt.getName())) {
								addError(
										"Error during " + MY_TYPE_NAME + " processing: schema for field '"
												+ field.getName() + "' already exests.", field);
							}
						}

						// create method schema descriptor
						final BlockStatement code = new BlockStatement();

						// create StringBuilder
						final Expression result = new VariableExpression("result");
						final Expression init = new ConstructorCallExpression(STRINGBUILDER_TYPE,
								MethodCallExpression.NO_ARGUMENTS);
						code.addStatement(declStatement(result, init));
						// append values
						code.addStatement(append(result, schemaFieldValue));
						code.addStatement(append(result, new ConstantExpression(".")));
						code.addStatement(append(result, new VariableExpression(SUBPATH_PARAM_NAME)));
						// return result by toString call
						MethodCallExpression toString = new MethodCallExpression(result, "toString",
								MethodCallExpression.NO_ARGUMENTS);
						toString.setImplicitThis(false);
						code.addStatement(new ReturnStatement(toString));

						Parameter param = new Parameter(ClassHelper.STRING_TYPE, SUBPATH_PARAM_NAME);
						MethodNode schemaStaticMethod = new MethodNode(schemaName, FieldNode.ACC_PUBLIC
								| FieldNode.ACC_STATIC, ClassHelper.STRING_TYPE, new Parameter[] {param},
								ClassNode.EMPTY_ARRAY, code);
						addMethods.add(schemaStaticMethod);
					}
				}
			}
		}

		for (FieldNode field : addFields) {
			cNode.addField(field);
		}
		for (MethodNode method : addMethods) {
			cNode.addMethod(method);
		}
	}

	private static ExpressionStatement append(Expression result, Expression expr) {
		MethodCallExpression append = new MethodCallExpression(result, "append", expr);
		append.setImplicitThis(false);

		return new ExpressionStatement(append);
	}

}
