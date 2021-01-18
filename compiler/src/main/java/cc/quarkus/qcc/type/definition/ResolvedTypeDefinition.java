package cc.quarkus.qcc.type.definition;

/**
 *
 */
public interface ResolvedTypeDefinition extends ValidatedTypeDefinition {
    // ==================
    // Lifecycle
    // ==================

    default ResolvedTypeDefinition validate() {
        return this;
    }

    default ResolvedTypeDefinition resolve() {
        return this;
    }

    // ==================
    // Superclass
    // ==================

    ResolvedTypeDefinition getSuperClass();

    // ==================
    // Interfaces
    // ==================

    ResolvedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;

    // ==================
    // Methods
    // ==================

    // ==================
    // Constructors
    // ==================
}
