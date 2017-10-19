package in.dragonbra.model;

/**
 * @author lngtr
 * @since 2017-10-17
 */
public class Operator {

    private String operatorName;

    private String operatorCode;

    private String telephonyName;

    private String countryName;

    private String countryCode;

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorCode() {
        return operatorCode;
    }

    public void setOperatorCode(String operatorCode) {
        this.operatorCode = operatorCode;
    }

    public String getTelephonyName() {
        return telephonyName;
    }

    public void setTelephonyName(String telephonyName) {
        this.telephonyName = telephonyName;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
