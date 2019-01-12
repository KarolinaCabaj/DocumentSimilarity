package postprocessing;

public enum QualityMeasure {
    GREAT, GOOD, BAD;

    public static QualityMeasure getQualityMeasure(Float value) {
        if (value < 1.0)
            return GREAT;
        else if (value < 3.0)
            return GOOD;
        else
            return BAD;
    }
}
