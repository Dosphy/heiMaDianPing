package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author Dosphy
 * @Date 2025/9/22 12:21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_score")
public class Score implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Long userId;

    private int score;

    public Score(Long userId, int i) {
        this.userId = userId;
        this.score = i;
    }
}
