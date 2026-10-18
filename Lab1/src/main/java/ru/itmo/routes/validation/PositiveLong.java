package ru.itmo.routes.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.*;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import ru.itmo.routes.service.NumericInputLimits;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@NotBlank
@Size(max = NumericInputLimits.max_length)
@Pattern(regexp = "[0-9]+")
@DecimalMin("1")
@DecimalMax("9223372036854775807")
public @interface PositiveLong {
    String message() default "Требуется положительное целое число в диапазоне long длиной не более 20 символов";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
