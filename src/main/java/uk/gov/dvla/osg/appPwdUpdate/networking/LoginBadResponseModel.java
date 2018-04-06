package uk.gov.dvla.osg.appPwdUpdate.networking;

import java.util.List;

/*
 * Models the Json response for RPD login errors.
 */
public class LoginBadResponseModel {

    private Object attributeErrors;
    private List<LoginError> generalErrors;

    public Object getAttributeErrors() {
        return attributeErrors;
    }

    public void setAttributeErrors(Object attributeErrors) {
        this.attributeErrors = attributeErrors;
    }

    public String getMessage() {
        return generalErrors.get(0).getMessage();
    }

    public String getAction() {
        return generalErrors.get(0).getAction();
    }
    
    public String getCode() {
        return generalErrors.get(0).getCode();
    }

    public void setGeneralErrors(List<LoginError> generalErrors) {
        this.generalErrors = generalErrors;
    }
}
