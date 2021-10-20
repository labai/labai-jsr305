package com.sun.tools.xjc.addon.labai;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CAttributePropertyInfo;
import com.sun.tools.xjc.model.CElementPropertyInfo;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CValuePropertyInfo;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.tools.xjc.outline.PackageOutline;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.impl.AttributeUseImpl;
import com.sun.xml.xsom.impl.ParticleImpl;
import org.xml.sax.ErrorHandler;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/*
 * Augustus, 2021-10-17
 *
 * Based on https://github.com/krasa/krasa-jaxb-tools
 */
public class JaxbNonnullPlugin extends Plugin {
    private static final String namespace = "http://jaxb.dev.java.net/plugin/code-injector";

    private static final String PLUGIN_OPTION_NAME = "XJsr305Annotations";
    private static final String PARAM_GENERATE_DEFAULT_NULLABLE = PLUGIN_OPTION_NAME + ":generateDefaultNullable";
    private static final String PARAM_DEFAULT_NULLABLE_CLASS = PLUGIN_OPTION_NAME + ":defaultNullableClass";
    private static final String PARAM_NONNULL_CLASS = PLUGIN_OPTION_NAME + ":nonnullClass";
    private static final String PARAM_VERBOSE = PLUGIN_OPTION_NAME + ":verbose";

    private static final String DEFAULT_NOTNULL_ANNOTATION = "javax.annotation.Nonnull";
    private static final String DEFAULT_NULLABLE_ANNOTATION = "com.github.labai.jsr305x.api.NullableByDefault";

    private boolean verbose = true;
    private boolean generatePackageDefault = true;
    private Class<? extends Annotation> packageDefaultClass = null;
    private Class<? extends Annotation> nonnullClass = null;

    @Override
    public String getOptionName() {
        return PLUGIN_OPTION_NAME;
    }

    @Override
    public int parseArgument(Options opt, String[] args, int i) throws BadCommandLineException {
        String arg1 = args[i];
        int consumed = 0;
        String v;

        // @NullableByDefault
        //
        v = getParameterValue(arg1, PARAM_GENERATE_DEFAULT_NULLABLE);
        if (v != null) {
            generatePackageDefault = Boolean.parseBoolean(v);
            consumed++;
        }

        v = getParameterValue(arg1, PARAM_DEFAULT_NULLABLE_CLASS);
        if (v != null) {
            consumed++;
        }
        if (v == null && generatePackageDefault) {
            v = DEFAULT_NULLABLE_ANNOTATION;
        }
        if (v != null) {
            try {
                packageDefaultClass = (Class<? extends Annotation>) Class.forName(v);
            } catch (Throwable e) {
                log(e);
                throw new BadCommandLineException("Invalid '" + PARAM_DEFAULT_NULLABLE_CLASS + "' value ('" + v + "') - must be Annotation class. Error: " + e.getMessage());
            }
        }

        // @Nonnull
        //
        v = getParameterValue(arg1, PARAM_NONNULL_CLASS);
        if (v != null)
            consumed++;
        else {
            v = DEFAULT_NOTNULL_ANNOTATION;
        }
        try {
            nonnullClass = (Class<? extends Annotation>) Class.forName(v);
        } catch (Throwable e) {
            log(e);
            throw new BadCommandLineException("Invalid '" + PARAM_NONNULL_CLASS + "' value ('" + v + "') - must be Annotation class. Error: " + e.getMessage());
        }

        // verbose
        //
        v = getParameterValue(arg1, PARAM_VERBOSE);
        if (v != null) {
            verbose = Boolean.parseBoolean(v);
            consumed++;
        }

        return consumed;
    }

    @Override
    public List<String> getCustomizationURIs() {
        return Collections.singletonList(namespace);
    }

    @Override
    public boolean isCustomizationTagName(String nsUri, String localName) {
        return nsUri.equals(namespace) && localName.equals("code");
    }

    @Override
    public void onActivated(Options opts) throws BadCommandLineException {
        super.onActivated(opts);
    }

    @Override
    public String getUsage() {
        return "  -XJsr305Annotations : inject Nonnull annotations (JSR 305)";
    }

    @Override
    public boolean run(Outline model, Options opt, ErrorHandler errorHandler) {
        if (generatePackageDefault) {
            for (PackageOutline po : model.getAllPackageContexts()) {
                log("Add @" + packageDefaultClass.getName() + " to package " + po._package().name());
                po._package().annotate(packageDefaultClass);
            }
        }
        try {
            for (ClassOutline co : model.getClasses()) {
                List<CPropertyInfo> properties = co.target.getProperties();

                for (CPropertyInfo property : properties) {
                    if (property instanceof CElementPropertyInfo) {
                        processElement((CElementPropertyInfo) property, co, model);
                    } else if (property instanceof CAttributePropertyInfo) {
                        processAttribute((CAttributePropertyInfo) property, co, model);
                    } else if (property instanceof CValuePropertyInfo) {
                        processAttribute((CValuePropertyInfo) property, co, model);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            log(e);
            return false;
        }
    }


    /*
     * XS:Element
     */
    public void processElement(CElementPropertyInfo property, ClassOutline classOutline, Outline model) {
        XSComponent schemaComponent = property.getSchemaComponent();
        ParticleImpl particle = (ParticleImpl) schemaComponent;

        // must be reflection because of cxf-codegen
        int minOccurs = Utils.toInt(Utils.getField("minOccurs", particle));
        boolean nillable = Utils.toBoolean(Utils.getField("nillable", particle.getTerm()));
        JFieldVar field = classOutline.implClass.fields().get(propertyName(property));

        // workaround for choices
        boolean required = property.isRequired();
        if ((minOccurs < 0 || minOccurs >= 1 && required && !nillable)
                || property.isCollection()
                ) {
            processNonnull(classOutline, field);
        }
    }


    private void processNonnull(ClassOutline co, JFieldVar field) {
        if (!hasAnnotation(field, nonnullClass)) {
            log("Add @" + nonnullClass.getSimpleName() + " on " + field.name() + " of class " + co.implClass.name());
            field.annotate(nonnullClass);
            // todo make non n^2 algorithm
            String capitalizedName = field.name().substring(0, 1).toUpperCase() + field.name().substring(1);
            String setterName = "set" + capitalizedName;
            String getterName = "get" + capitalizedName;
            String booleanName = "is" + capitalizedName;
            JMethod getter = co.implClass.methods().stream()
                    .filter(it -> getterName.equals(it.name()) || booleanName.equals(it.name()))
                    .filter(it -> it.params().size() == 0)
                    .findFirst().orElse(null);
            JMethod setter = co.implClass.methods().stream()
                    .filter(it -> setterName.equals(it.name()))
                    .filter(it -> it.params().size() == 1)
                    .findFirst().orElse(null);
            if (getter != null)
                getter.annotate(nonnullClass);
            if (setter != null)
                setter.params().get(0).annotate(nonnullClass);
        }
    }


    /* attribute from parent declaration */
    private void processAttribute(CValuePropertyInfo property, ClassOutline clase, Outline model) {
    }

    /* XS:Attribute */
    public void processAttribute(CAttributePropertyInfo property, ClassOutline clase, Outline model) {
        String propertyName = property.getName(false);

        XSComponent definition = property.getSchemaComponent();
        AttributeUseImpl particle = (AttributeUseImpl) definition;

        JFieldVar var = clase.implClass.fields().get(propertyName);
        if (particle.isRequired()) {
            processNonnull(clase, var);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean hasAnnotation(JFieldVar fvar, Class annotationClass) {
        List<JAnnotationUse> list = (List<JAnnotationUse>) Utils.getField("annotations", fvar);
        if (list != null) {
            for (JAnnotationUse annotationUse : list) {
                if (((Class) Utils.getField("clazz._class", annotationUse)).getCanonicalName().equals(
                        annotationClass.getCanonicalName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String propertyName(CElementPropertyInfo property) {
        return property.getName(false);
    }


    private void log(Throwable e) {
        e.printStackTrace();
    }

    private void log(String log) {
        if (verbose) {
            System.out.println("JaxbJsr305: "+ log);
        }
    }

    private static String getParameterValue(String args, String name) {
        int idx = args.indexOf(name);
        return idx > 0 ? args.substring(idx + name.length() + 1) : null;
    }
}
