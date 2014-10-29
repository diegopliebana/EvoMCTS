package controllers.sampleMCTS.learning;

import java.util.Arrays;

// Copied and hacked from from Apache Math

public class ArrayRealVector  {

    /** Entries of the vector. */
    private double data[];


    public ArrayRealVector() {
        data = new double[0];
    }

    /**
     * Construct a vector of zeroes.
     *
     * @param size Size of the vector.
     */
    public ArrayRealVector(int size) {
        data = new double[size];
    }

    /**
     * Construct a vector with preset values.
     *
     * @param size Size of the vector
     * @param preset All entries will be set with this value.
     */
    public ArrayRealVector(int size, double preset) {
        data = new double[size];
        Arrays.fill(data, preset);
    }

    /**
     * Construct a vector from an array, copying the input array.
     *
     * @param d Array.
     */
    public ArrayRealVector(double[] d) {
        data = d.clone();
    }

    public ArrayRealVector(double[] d, boolean copyArray)
             {

        data = copyArray ? d.clone() :  d;
    }



    /**
     * Construct a vector from an array.
     *
     * @param d Array of {@code Double}s.
     */
    public ArrayRealVector(Double[] d) {
        data = new double[d.length];
        for (int i = 0; i < d.length; i++) {
            data[i] = d[i].doubleValue();
        }
    }


    public ArrayRealVector copy() {
        return new ArrayRealVector(this.data, true);
    }


    public ArrayRealVector add(ArrayRealVector v) throws DimensionMismatchException {
            final double[] vData = ((ArrayRealVector) v).data;
            final int dim = vData.length;
            checkVectorDimensions(dim);
            ArrayRealVector result = new ArrayRealVector(dim);
            double[] resultData = result.data;
            for (int i = 0; i < dim; i++) {
                resultData[i] = data[i] + vData[i];
            }
            return result;

    }


    public ArrayRealVector subtract(ArrayRealVector v) throws DimensionMismatchException {
        final double[] vData = ((ArrayRealVector) v).data;
        final int dim = vData.length;
        checkVectorDimensions(dim);
        ArrayRealVector result = new ArrayRealVector(dim);
        double[] resultData = result.data;
        for (int i = 0; i < dim; i++) {
            resultData[i] = data[i] - vData[i];
        }
        return result;
    }


    public ArrayRealVector ebeMultiply(ArrayRealVector v) throws DimensionMismatchException {
        final double[] vData = ((ArrayRealVector) v).data;
        final int dim = vData.length;
        checkVectorDimensions(dim);
        ArrayRealVector result = new ArrayRealVector(dim);
        double[] resultData = result.data;
        for (int i = 0; i < dim; i++) {
            resultData[i] = data[i] * vData[i];
        }
        return result;
    }


    public ArrayRealVector ebeDivide(ArrayRealVector v) throws DimensionMismatchException {
        final double[] vData = ((ArrayRealVector) v).data;
        final int dim = vData.length;
        checkVectorDimensions(dim);
        ArrayRealVector result = new ArrayRealVector(dim);
        double[] resultData = result.data;
        for (int i = 0; i < dim; i++) {
            resultData[i] = data[i] / vData[i];
        }
        return result;
    }

    /**
     * Get a reference to the underlying data array.
     * This method does not make a fresh copy of the underlying data.
     *
     * @return the array of entries.
     */
    public double[] getDataRef() {
        return data;
    }


    public double dotProduct(ArrayRealVector v) throws DimensionMismatchException {
            final double[] vData = ((ArrayRealVector) v).data;
            checkVectorDimensions(vData.length);
            double dot = 0;
            for (int i = 0; i < data.length; i++) {
                dot += data[i] * vData[i];
            }
            return dot;

     }

    protected void checkVectorDimensions(ArrayRealVector v)
            throws DimensionMismatchException {
        checkVectorDimensions(v.getDimension());
    }

    protected void checkVectorDimensions(int n)
            throws DimensionMismatchException {
        if (data.length != n) {
            throw new DimensionMismatchException(data.length, n);
        }
    }


    public int getDimension() {
        return data.length;
    }

    public double getEntry(int index)  {
       return data[index];

    }


    public void setEntry(int index, double value)  {
        data[index] = value;

    }
}
