package org.springframework.boot.logging.logback;

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.conditional.Condition;
import ch.qos.logback.core.joran.conditional.PropertyWrapperForScripts;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import org.codehaus.janino.ClassBodyEvaluator;

import org.springframework.core.NativeDetector;
import org.springframework.util.ClassUtils;

class AotIfModelHandler extends ModelHandlerBase {

	private static int conditionCount;

	private static final Map<String, CompiledCondition> compiledConditions = new HashMap<>();

	private static String SCRIPT_PREFIX = "" + "public boolean evaluate() { return ";

	private static String SCRIPT_SUFFIX = "" + "; }";

	public AotIfModelHandler(Context context) {
		super(context);
	}

	@Override
	public void handle(ModelInterpretationContext intercon, Model model) throws ModelHandlerException {
		IfModel ifModel = (IfModel) model;
		intercon.pushModel(ifModel);
		Condition condition = conditionFor(ifModel);
		((PropertyWrapperForScripts) condition).setPropertyContainers(intercon, this.context);
		ifModel.setBranchState(condition.evaluate());
	}

	private Condition conditionFor(IfModel model) {
		Class<Condition> conditionClass = !NativeDetector.inNativeImage() ? compileCondition(model)
				: loadCondition(model);
		try {
			return conditionClass.getConstructor().newInstance();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@SuppressWarnings("unchecked")
	private Class<Condition> compileCondition(IfModel model) {
		ClassBodyEvaluator cbe = new ClassBodyEvaluator();
		cbe.setImplementedInterfaces(new Class[] { Condition.class });
		cbe.setExtendedClass(PropertyWrapperForScripts.class);
		cbe.setParentClassLoader(ClassBodyEvaluator.class.getClassLoader());
		String className = nextConditionClassName();
		cbe.setClassName(className);
		try {
			cbe.cook(SCRIPT_PREFIX + model.getCondition() + SCRIPT_SUFFIX);
			if (isAotProcessingInProgress()) {
				byte[] bytecode = cbe.getBytecodes().get(className);
				compiledConditions.put(model.getCondition(), new CompiledCondition(className, bytecode));
			}
			return (Class<Condition>) cbe.getClazz();
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private String nextConditionClassName() {
		return "org.springframework.boot.logging.logback.Condition" + conditionCount++;
	}

	private boolean isAotProcessingInProgress() {
		return Boolean.getBoolean("spring.aot.processing");
	}

	@SuppressWarnings("unchecked")
	private Class<Condition> loadCondition(IfModel model) {
		try {
			return (Class<Condition>) ClassUtils.forName(nextConditionClassName(), getClass().getClassLoader());
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	static Map<String, CompiledCondition> compiledConditions() {
		return compiledConditions;
	}

	static record CompiledCondition(String className, byte[] bytecode) {

	}

}
