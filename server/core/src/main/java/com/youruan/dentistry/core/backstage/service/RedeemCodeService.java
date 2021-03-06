
package com.youruan.dentistry.core.backstage.service;

import com.youruan.dentistry.core.backstage.domain.RedeemCode;
import com.youruan.dentistry.core.backstage.query.RedeemCodeQuery;
import com.youruan.dentistry.core.backstage.vo.ExtendedRedeemCode;
import com.youruan.dentistry.core.base.query.Pagination;

import java.util.List;

public interface RedeemCodeService {

    /**
     * 根据id，获取单条记录
     */
    public RedeemCode get(Long id);
    /**
     * 根据条件，获取单条记录
     */
    public ExtendedRedeemCode queryOne(RedeemCodeQuery qo);
    /**
     * 返回所有记录
     */
    public List<ExtendedRedeemCode> listAll(RedeemCodeQuery qo);
    /**
     * 根据条件，查询列表
     */
    public Pagination<ExtendedRedeemCode> query(RedeemCodeQuery qo);
    /**
     * 根据查询条件，返回记录条目
     */
    public int count(RedeemCodeQuery qo);
    /**
     * 添加
     */
    void create(Long productId, Long shopId, Integer amount);
    /**
     * 根据id集合，查询对应列表
     */
    List<? extends RedeemCode> listAll(Long[] dictionaryIds);
    /**
     * 返回所有记录
     */
    List<ExtendedRedeemCode> listAll();

    /**
     * 根据code获取记录
     */
    RedeemCode getByCode(String code);

    /**
     * 用户兑换
     */
    void bindUser(RedeemCode redeemCode, Long userId, String dicItemName);

    /**
     * 兑换完成
     */
    void redeemCompleted(ExtendedRedeemCode redeemCode);
}
