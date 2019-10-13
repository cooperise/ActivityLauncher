package com.kyle.support.activitystarter.compiler;

import com.google.auto.service.AutoService;
import com.kyle.support.activitystarter.annotation.Optional;
import com.kyle.support.activitystarter.annotation.Required;
import com.kyle.support.activitystarter.annotation.StarterBuilder;
import com.kyle.support.activitystarter.compiler.generator.CodeGenerator;
import com.kyle.support.activitystarter.compiler.generator.ActivityStarterGenerator;
import com.squareup.javapoet.JavaFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

@AutoService(Processor.class)
public class ActivityStarterProcessor extends AbstractProcessor {

    private Map<String, CodeGenerator> mCodeGeneratorMap = new HashMap<>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        supportTypes.add(StarterBuilder.class.getCanonicalName());
        supportTypes.add(Required.class.getCanonicalName());
        supportTypes.add(Optional.class.getCanonicalName());
        return supportTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        // To handle StarterBuilder annotations
        Set<? extends Element> starterBuilders = roundEnvironment.getElementsAnnotatedWith(StarterBuilder.class);
        for (Element element : starterBuilders) {
            TypeElement classElement = (TypeElement) element;
            String fullClassName = classElement.getQualifiedName().toString();
            CodeGenerator generator = mCodeGeneratorMap.get(fullClassName);
            if (generator == null) {
                generator = new ActivityStarterGenerator(processingEnv.getElementUtils().getPackageOf(classElement), classElement);
                mCodeGeneratorMap.put(fullClassName, generator);
            }
        }
        // To handle Required annotations
        Set<? extends Element> requiredElements = roundEnvironment.getElementsAnnotatedWith(Required.class);
        for (Element element : requiredElements) {
            VariableElement variableElement = (VariableElement) element;
            String fullClassName = ((TypeElement) variableElement.getEnclosingElement()).getQualifiedName().toString();
            CodeGenerator generator = mCodeGeneratorMap.get(fullClassName);
            if (generator != null) {
                generator.putRequiredElement(variableElement);
            }
        }
        // To handle Optional annotations
        Set<? extends Element> optionalElements = roundEnvironment.getElementsAnnotatedWith(Optional.class);
        for (Element element : optionalElements) {
            VariableElement variableElement = (VariableElement) element;
            String fullClassName = ((TypeElement) variableElement.getEnclosingElement()).getQualifiedName().toString();
            CodeGenerator generator = mCodeGeneratorMap.get(fullClassName);
            if (generator != null) {
                generator.putOptionalElement(variableElement);
            }
        }
        for (String key : mCodeGeneratorMap.keySet()) {
            CodeGenerator generator = mCodeGeneratorMap.get(key);
            JavaFile javaFile = JavaFile.builder(generator.getPackageName(), generator.generateCode()).build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
                System.out.println("javaFile >>>>> " + javaFile.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mCodeGeneratorMap.clear();
        return true;
    }
}
