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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

/** 
 * This annotation processor examines classes marked {@link com.jasonstedman.extensions.Immutable}
 * 
 * @author Jason Stedman
 * @version 1.0
 * 
 */
@SupportedAnnotationTypes("com.jasonstedman.extensions.Immutable")
public class ImmutableAnnotationProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment environment) {
		Set<? extends Element> rootElements = environment.getRootElements();
		for (Element element : rootElements)
		{	
			try{ 
				TypeElement classElement = (TypeElement) element;
				@SuppressWarnings("unchecked")
				List<TypeParameterElement> typeParams = (List<TypeParameterElement>) classElement.getTypeParameters();
				for(TypeParameterElement e : typeParams){
					for(TypeMirror extendsType : e.getBounds()){
						if(!ImmutabilityValidator.elementOrSuperClassesAreMarkedImmutable(
								processingEnv.getTypeUtils().asElement(extendsType),processingEnv)){
							processingEnv.getMessager().printMessage(Kind.ERROR, "Class " +element.toString() + 
									" but type parameter " + e.toString() + " does not extend an immutable class.", element);
						}
					}
				}
				if(ImmutabilityValidator.elementOrSuperClassesAreMarkedImmutable(element, processingEnv))
					ImmutabilityValidator.ensureElementIsImmutable(element, element, processingEnv);
			}catch(Exception e){
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintWriter writer = new PrintWriter(out);
				e.printStackTrace(writer);
				writer.close();
				processingEnv.getMessager().printMessage(Kind.ERROR, new String(out.toByteArray()) , element);
			}

		}

		return false;
	}



}
