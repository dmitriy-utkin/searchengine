package searchengine.dto.indexing;

import com.sun.istack.NotNull;
import jdk.jfr.BooleanFlag;
import lombok.Value;

public enum IndexResponseImpl {;

    private interface Result{@BooleanFlag Boolean getResult(); }
    private interface Error{@NotNull String getError(); }

    public enum Response {;

        @Value public static class SuccessResponse implements IndexResponse, Result {
            public SuccessResponse() {
                this.result = true;
            }
            Boolean result;
        }

        @Value public static class BadRequest implements IndexResponse, Result, Error {
            public BadRequest(String error) {
                this.error = error;
                this.result = false;
            }
            Boolean result;
            String error;
        }

    }

}
