package org.petspa.petcaresystem.schedule.model.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResponseObj {
    String message;
//    String status;
    Object data;
}
