package com.schlimm.springcdi.interceptor.strategies.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.MethodMetadata;

import com.schlimm.springcdi.interceptor.InterceptorAwareBeanFactoryPostProcessorException;
import com.schlimm.springcdi.interceptor.InterceptorModuleUtils;
import com.schlimm.springcdi.interceptor.model.InterceptorInfo;
import com.schlimm.springcdi.interceptor.model.MethodInterceptorInfo;
import com.schlimm.springcdi.interceptor.strategies.InterceptorResolutionStrategy;

public class SimpleInterceptorResolutionStrategy implements InterceptorResolutionStrategy {

	private static final String SCOPED_TARGET = "scopedTarget.";

	private ArrayList<InterceptorInfo> registeredInterceptorsCache;

	@Override
	public List<InterceptorInfo> resolveRegisteredInterceptors(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) 
			throw new InterceptorAwareBeanFactoryPostProcessorException("Simple interceptor strategy only supports ConfigurableListableBeanFactory");
		ConfigurableListableBeanFactory configurableListableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
		List<InterceptorInfo> interceptors = new ArrayList<InterceptorInfo>();
		if (registeredInterceptorsCache == null) {
			registeredInterceptorsCache = new ArrayList<InterceptorInfo>();
			String[] bdNames = configurableListableBeanFactory.getBeanDefinitionNames();
			for (String bdName : bdNames) {
				BeanDefinition bd = configurableListableBeanFactory.getBeanDefinition(bdName);
				if (bd instanceof AnnotatedBeanDefinition) {
					AnnotatedBeanDefinition abd = (AnnotatedBeanDefinition) bd;
					if (InterceptorInfo.isInterceptor(abd)) {
						if (bdName.startsWith(SCOPED_TARGET)) {
							bd = configurableListableBeanFactory.getBeanDefinition(bdName.replace(SCOPED_TARGET, ""));
						}
						if (bd.isAutowireCandidate()) {
							InterceptorInfo interceptorInfo = new MethodInterceptorInfo(new BeanDefinitionHolder(bd, bdName.replace(SCOPED_TARGET, "")));
							resolveInterceptorTargets(configurableListableBeanFactory, interceptorInfo);
							interceptors.add(interceptorInfo);
						} 
					}
				}
			}
		} else {
			interceptors = registeredInterceptorsCache;
		}
		return interceptors;
	}

	@SuppressWarnings("unchecked")
	private void resolveInterceptorTargets(BeanFactory beanFactory, InterceptorInfo interceptorInfo) {
		List<String> interceptorBindings = interceptorInfo.getInterceptorBindings();
		ConfigurableListableBeanFactory configurableListableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
		String[] bnNames = configurableListableBeanFactory.getBeanDefinitionNames();
		for (String bdName : bnNames) {
			BeanDefinition bd = configurableListableBeanFactory.getBeanDefinition(bdName);
			if (bd instanceof AnnotatedBeanDefinition && !InterceptorInfo.isInterceptor((AnnotatedBeanDefinition) bd)) {
 				AnnotatedBeanDefinition abd = (AnnotatedBeanDefinition) bd;
				for (String binding : interceptorBindings) {
					if (abd.getMetadata().hasAnnotation(binding)){
						interceptorInfo.addInterceptedBean(bdName);
						interceptorInfo.addClassLevelInterception(bdName);
					}
					if (abd.getMetadata().hasAnnotatedMethods(binding)){
						interceptorInfo.addInterceptedBean(bdName);
						Set<MethodMetadata> methods = abd.getMetadata().getAnnotatedMethods(binding);
						for (MethodMetadata methodMetadata : methods) {
							Set<Method> jlrMethods = InterceptorModuleUtils.getMethodsForName(InterceptorModuleUtils.getClass_forName(methodMetadata.getDeclaringClassName()),methodMetadata.getMethodName());
							for (Method method : jlrMethods) {
								if (AnnotationUtils.findAnnotation(method, InterceptorModuleUtils.getClass_forName(binding))!=null) {
									interceptorInfo.addInterceptedMethod(method);
								}
							}
						}
					}
				}
			}
		}
	}
	
	

}