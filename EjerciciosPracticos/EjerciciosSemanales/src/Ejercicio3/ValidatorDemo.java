package Ejercicio3;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.*;

record ValidationResult(boolean isValid, List<String> errors) {
    static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }

    static ValidationResult invalid(String... errors) {
        return new ValidationResult(false, List.of(errors));
    }

    ValidationResult merge(ValidationResult other) {
        if (this.isValid && other.isValid) return valid();
        List<String> allErrors = new ArrayList<>(this.errors);
        allErrors.addAll(other.errors);
        return new ValidationResult(false, allErrors);
    }
}

@FunctionalInterface
interface Validator<T> {
    ValidationResult validate(T value);

    default Validator<T> and(Validator<T> other) {
        return value -> {
            ValidationResult r1 = this.validate(value);
            ValidationResult r2 = other.validate(value);
            return r1.merge(r2);
        };
    }

    static <T> Validator<T> from(Predicate<T> predicate, String errorMsg) {
        return value -> predicate.test(value)
                ? ValidationResult.valid()
                : ValidationResult.invalid(errorMsg);
    }
}

record User(String name, String email, int age) {}

public class ValidatorDemo {
    public static void main(String[] args) {
        Validator<String> notBlank = Validator.from(
                s -> s != null && !s.isBlank(), "No debe estar vacio");
        Validator<String> maxLen50 = Validator.from(
                s -> s.length() <= 50, "Maximo 50 caracteres");
        Validator<String> hasAt = Validator.from(
                s -> s.contains("@"), "Email debe contener @");
        Validator<Integer> minAge = Validator.from(
                a -> a >= 18, "Edad minima: 18");
        Validator<Integer> maxAge = Validator.from(
                a -> a <= 120, "Edad maxima: 120");

        Validator<String> nameValidator  = notBlank.and(maxLen50);
        Validator<String> emailValidator = notBlank.and(hasAt);
        Validator<Integer> ageValidator  = minAge.and(maxAge);

        Validator<User> userValidator = user -> {
            ValidationResult nameResult  = nameValidator.validate(user.name());
            ValidationResult emailResult = emailValidator.validate(user.email());
            ValidationResult ageResult   = ageValidator.validate(user.age());
            return nameResult.merge(emailResult).merge(ageResult);
        };

        System.out.println("=== Validacion de Strings ===");
        System.out.println("Nombre 'Ana': " + nameValidator.validate("Ana"));
        System.out.println("Nombre '': "   + nameValidator.validate(""));

        System.out.println("\n=== Validacion de Email ===");
        System.out.println("Email 'ana@mail.com': " + emailValidator.validate("ana@mail.com"));
        System.out.println("Email 'invalido': "     + emailValidator.validate("invalido"));

        System.out.println("\n=== Validacion de Edad ===");
        System.out.println("Edad 25: "  + ageValidator.validate(25));
        System.out.println("Edad 15: "  + ageValidator.validate(15));
        System.out.println("Edad 150: " + ageValidator.validate(150));

        System.out.println("\n=== Validacion de User Completo ===");
        User valid   = new User("Ana Garcia", "ana@mail.com", 25);
        User invalid = new User("", "sinArroba", 15);
        System.out.println("User valido: "   + userValidator.validate(valid));
        System.out.println("User invalido: " + userValidator.validate(invalid));
    }
}