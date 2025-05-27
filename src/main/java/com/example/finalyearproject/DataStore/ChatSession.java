//package com.example.finalyearproject.entities;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.time.LocalDateTime;
//
//@Getter
//@Setter
//@Entity
//@Table(name = "chat_session")
//public class ChatSession {
//    @Id
//    @Column(name = "id", nullable = false)
//    private Integer ChatSessionId;
//    private Integer FarmerId;
//    @ManyToOne
//    @JoinColumn(name = "ConsumerId")
//    private Integer ConsumerId;
//    @Temporal(TemporalType.TIMESTAMP)
//    private LocalDateTime StartTime;
//    @Temporal(TemporalType.TIMESTAMP)
//    private LocalDateTime EndTime;



//}