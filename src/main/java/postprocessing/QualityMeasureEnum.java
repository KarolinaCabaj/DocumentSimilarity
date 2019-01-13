package postprocessing;

public enum QualityMeasureEnum {
    GREAT, GOOD, BAD;

    public static QualityMeasureEnum getQualityMeasure(Double value) {
        if (value < 1.0)
            return GREAT;
        else if (value < 3.0)
            return GOOD;
        else
            return BAD;
    }
}
