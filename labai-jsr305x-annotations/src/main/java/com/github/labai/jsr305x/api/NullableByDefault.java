package com.github.labai.jsr305x.api;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;
import java.lang.annotation.ElementType;

/**
 * @author Augustus
 * created on 2021.10.12
 */

@Nonnull(when = When.MAYBE)
@TypeQualifierDefault({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
public @interface NullableByDefault {
}
