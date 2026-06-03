package com.yj.sparepart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yj.sparepart.entity.Substitution;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 替代关系 Mapper
 * 支持双向查找可替代备件
 */
@Mapper
public interface SubstitutionMapper extends BaseMapper<Substitution> {

    /**
     * 双向查找指定备件的所有可替代备件（去重、取最小优先级）
     * 替代关系无向，因此分别查询 source_id = partId 和 target_id = partId 的记录，
     * 然后按替代备件ID分组，取最小优先级，并按优先级升序返回。
     *
     * @param partId 当前备件ID
     * @return 可替代备件ID及其优先级的列表（已按优先级排序）
     */
    @Select("SELECT t.alt_id as targetId, MIN(t.priority) as priority FROM (" +
            "  SELECT target_id AS alt_id, priority FROM substitution WHERE source_id = #{partId} " +
            "  UNION ALL " +
            "  SELECT source_id AS alt_id, priority FROM substitution WHERE target_id = #{partId} " +
            ") t GROUP BY t.alt_id ORDER BY MIN(t.priority) ASC")
    List<SubstituteDTO> findSubstitutes(@Param("partId") Long partId);

    /**
     * 内部DTO，用于映射替代查询结果
     */
    class SubstituteDTO {
        /**
         * 可替代备件ID
         */
        private Long targetId;

        /**
         * 该替代备件的优先级（数字越小优先级越高）
         */
        private Integer priority;

        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }
}