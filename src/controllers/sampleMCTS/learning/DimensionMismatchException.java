package controllers.sampleMCTS.learning;

// Copied from Apache Math

public class DimensionMismatchException extends RuntimeException {
    /** Serializable version Id. */
    private static final long serialVersionUID = -8415396756375798143L;
    /** Correct dimension. */
    private final int dimension;

    /**
     * Construct an exception from the mismatched dimensions.
     *
     * @param specific Specific context information pattern.
     * @param wrong Wrong dimension.
     * @param expected Expected dimension.
     */
    public DimensionMismatchException(String specific,
                                      int wrong,
                                      int expected) {
        super(specific + " " + wrong + " , expected"  + expected ) ;
        dimension = expected;
    }

    /**
     * Construct an exception from the mismatched dimensions.
     *
     * @param wrong Wrong dimension.
     * @param expected Expected dimension.
     */
    public DimensionMismatchException(int wrong,
                                      int expected) {
        this("Wrong size", wrong, expected);
    }

    /**
     * @return the expected dimension.
     */
    public int getDimension() {
        return dimension;
    }
}
