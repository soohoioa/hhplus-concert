package kr.hhplus.be.server.point.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.common.error.AppException;
import kr.hhplus.be.server.common.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_point")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint {

    @Id
    private Long userId;

    @Column(nullable = false)
    private Long balance;

    @Version
    private Long version;

    public static UserPoint init(Long userId) {
        UserPoint p = new UserPoint();
        p.userId = userId;
        p.balance = 0L;
        return p;
    }

    public void charge(long amount) {
        if (amount <= 0) throw new AppException(ErrorCode.INVALID_REQUEST);
        this.balance += amount;
    }

    public void spend(long amount) {
        if (amount <= 0) throw new AppException(ErrorCode.INVALID_REQUEST);
        if (this.balance < amount) throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        this.balance -= amount;
    }
}
