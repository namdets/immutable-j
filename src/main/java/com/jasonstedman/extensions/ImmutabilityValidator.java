/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Jason Stedman
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.jasonstedman.extensions;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

/** 
 * This utility class has public methods useful for determining
 * if an element meets the requirements for immutability as 
 * enforced by this library.
 * 
 * @author Jason Stedman
 * @version 1.1
 * 
 */
public class ImmutabilityValidator {

	/**
	 * This method ensures that an element is immutable. If an element or it's enclosed 
	 * elements are not immutable, an error message is generated using the messenger of the 
	 * ProcessingEnvironment that is passed in. Generated messages specify the class being 
	 * processed, the element that is not immutable, and a generic message indicating that 
	 * the element specified is not final and either primitive or immutable.
	 * 
	 * @param element
	 * @param rootElement
	 * @param processingEnv
	 */
	public static void ensureElementIsImmutable(final Element element, final Element rootElement
			, final ProcessingEnvironment processingEnv) {
		checkImmutability(element, rootElement, processingEnv);
		for(Element e : ElementFilter.fieldsIn(element.getEnclosedElements())){
			ensureElementIsImmutable(e, element, processingEnv);
		}
	}

	/**
	 * This method exists out of courtesy for the Eclipse community in relation to:
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=270754
	 * 
	 * Maven correctly handles @Inherited annotations. Eclipse currently does not
	 * handle @Inherited consistently. To ensure that this processor behaves correctly
	 * in what is the standard IDE for java development, I have implemented this workaround.
	 * 
	 * Note: This may only be effective when some class in the current round is marked
	 * immutable as otherwise the ImmutableAnnotationProcessor may not be called at all.
	 * 
	 * @param element
	 * @param processingEnv
	 * @return
	 */
	public static boolean elementOrSuperClassesAreMarkedImmutable(Element element
			, final ProcessingEnvironment processingEnv) {
		Types typeUtils = processingEnv.getTypeUtils();
		Element elementTypeAsElement = typeUtils.asElement(element.asType());
		if(elementTypeAsElement instanceof TypeElement){
			return typeElementIsAnnotatedImmutable(typeUtils, elementTypeAsElement);
		}else if(elementTypeAsElement instanceof TypeParameterElement){
			return typeParameterElementIsAnnotatedImmutable(typeUtils,
					elementTypeAsElement);
		}

		return false;
	}

	public static boolean elementOrSuperClassesAreMarkedImmutableTypeParameters(Element element
			, final ProcessingEnvironment processingEnv) {
		Types typeUtils = processingEnv.getTypeUtils();
		Element elementTypeAsElement = typeUtils.asElement(element.asType());
		if(elementTypeAsElement instanceof TypeElement){
			return typeElementIsAnnotatedImmutableTypeParameters(typeUtils, elementTypeAsElement);
		}else if(elementTypeAsElement instanceof TypeParameterElement){
			return typeParameterElementIsAnnotatedImmutable(typeUtils,
					elementTypeAsElement);
		}

		return false;
	}

	public static boolean isImmutableBuiltInClass(String string) {
		for(@SuppressWarnings("rawtypes") Class c : IMMUTABLE_CLASSES){
			if(string.equals(c.getCanonicalName())) return true;
		}
		return false;
	}

	public static <T> boolean isImmutableClass(Class<T> clazz){
		System.out.println(clazz.getCanonicalName());
		return clazz.isAnnotationPresent(Immutable.class) || isImmutableBuiltInClass(clazz.getCanonicalName());
	}
	
	private static boolean typeParameterElementIsAnnotatedImmutable(
			Types typeUtils, Element elementTypeAsElement) {
		TypeMirror aClass = elementTypeAsElement.asType();
		boolean allTypeParamsImmutable = true;
		List<? extends TypeMirror> directSupertypes = typeUtils.directSupertypes(aClass);
		for(TypeMirror typeParameterExtends : directSupertypes){
			TypeElement typeParameterExtendsClassElement = (TypeElement) typeUtils.asElement(typeParameterExtends);
			boolean currentTypeParameterOnlyExtendsImmutableTypes = false;
			while(typeParameterExtends.getKind()!=TypeKind.NONE){
				if(typeParameterExtendsClassElement.getAnnotation(Immutable.class)!=null){
					currentTypeParameterOnlyExtendsImmutableTypes = true;
					break;
				}
				typeParameterExtends = typeParameterExtendsClassElement.getSuperclass();
				typeParameterExtendsClassElement = (TypeElement)typeUtils.asElement(typeParameterExtends);					
			}
			if(currentTypeParameterOnlyExtendsImmutableTypes==false){
				allTypeParamsImmutable = false;
				break;
			}
		}
		return allTypeParamsImmutable;
	}

	private static boolean typeElementIsAnnotatedImmutable(Types typeUtils,
			Element elementTypeAsElement) {
		TypeElement classElement = (TypeElement) elementTypeAsElement;
		TypeMirror aClass = classElement.asType();
		while(aClass.getKind()!=TypeKind.NONE){
			if(classElement.getAnnotation(Immutable.class)!=null){
				return true;
			}
			aClass = classElement.getSuperclass();
			classElement = (TypeElement)typeUtils.asElement(aClass);
		}
		return false;
	}

	private static boolean typeElementIsAnnotatedImmutableTypeParameters(Types typeUtils,
			Element elementTypeAsElement) {
		TypeElement classElement = (TypeElement) elementTypeAsElement;
		TypeMirror aClass = classElement.asType();
		while(aClass.getKind()!=TypeKind.NONE){
			if(classElement.getAnnotation(ImmutableTypeParameters.class)!=null){
				return true;
			}
			aClass = classElement.getSuperclass();
			classElement = (TypeElement)typeUtils.asElement(aClass);
		}
		return false;
	}

	private static void checkImmutability(final Element element, final Element rootElement
			, final ProcessingEnvironment processingEnv) {
		Messager messager = processingEnv.getMessager();

		if(element.getKind().isField()){
			if(!isImmutable(element, processingEnv)){
				Element typeElement = processingEnv.getTypeUtils().asElement(element.asType());
				messager.printMessage(
						Kind.ERROR, 
						"Class "+ rootElement.toString() +" marked @Immutable but element " 
						+ element.getSimpleName()+ " of type " + getElementTypeName(typeElement) 
						+ " is not final and either primitive or immutable.",
						element);	
			}
		}

	}

	private static String getElementTypeName(Element typeElement) {
		String elementTypeName = typeElement.toString();
		if(typeElement instanceof TypeElement){
			elementTypeName = ((TypeElement)typeElement).getQualifiedName().toString();
		}else if(typeElement instanceof TypeParameterElement){
			elementTypeName = ((TypeParameterElement)typeElement).getSimpleName().toString();	
		}
		return elementTypeName;
	}

	public static boolean isImmutable(Element element, final ProcessingEnvironment processingEnv) {
		boolean isImmutable = false;

		isImmutable = (
				element.getModifiers().contains(Modifier.FINAL) &&
				(
						isImmutableBuiltInClass(element.asType().toString()) ||
						isPrimitive(element, processingEnv) ||
						elementOrSuperClassesAreMarkedImmutable(element, processingEnv)
						)
				);

		return isImmutable;
	}

	private static boolean isPrimitive(Element element, final ProcessingEnvironment processingEnv) {
		return element.asType().getKind().isPrimitive();
	}

	/*
	 * These classes are considered immutable. There may be some that have 
	 * been missed but would still be useful. Please pull request if you 
	 * add any new ones that are part of the standard libraries.
	 */
	@SuppressWarnings("rawtypes")
	private static final Class[] IMMUTABLE_CLASSES = {
		String.class,
		Boolean.class,
		Integer.class,
		Character.class,
		Byte.class,
		Short.class,
		Double.class,
		Long.class,
		Float.class,
		BigDecimal.class,
		BigInteger.class,
		Color.class,
		Font.class,
		Locale.class,
		File.class,
		UUID.class,
		URL.class,
		URI.class,
		Inet4Address.class,
		Inet6Address.class,
		InetSocketAddress.class
	};

}