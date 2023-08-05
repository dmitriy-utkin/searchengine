package searchengine.services;

import com.sun.istack.NotNull;
import jdk.jfr.BooleanFlag;
import lombok.Value;

public enum ResponseServiceImpl {;

    private interface Result{@BooleanFlag Boolean getResult(); }
    private interface Error{@NotNull String getError(); }

    public enum Response {;

        @Value public static class SuccessResponseService implements ResponseService, Result {
            public SuccessResponseService() {
                this.result = true;
            }
            Boolean result;
        }

        @Value public static class BadRequest implements ResponseService, Result, Error {
            public BadRequest(String error) {
                this.error = error;
                this.result = false;
            }
            Boolean result;
            String error;
        }

    }

}
