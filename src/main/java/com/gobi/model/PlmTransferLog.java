package com.gobi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Table(name = "plm_transfer_log")
public class PlmTransferLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date transferDate;

    private String fileName;
    private Boolean isSuccessful;
    private String responseMessage;
    private Boolean isRetransfer;
}
