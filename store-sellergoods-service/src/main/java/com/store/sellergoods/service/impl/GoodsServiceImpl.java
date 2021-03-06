package com.store.sellergoods.service.impl;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.store.mapper.*;
import com.store.pojo.*;
import com.store.pojogroup.Goods;
import com.store.sellergoods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;


import entity.PageResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
@Transactional//事物回滚 可以再方法上添加也可以在类上面增加 对于多表操作的问题
public class GoodsServiceImpl implements GoodsService {

	@Autowired
	private TbGoodsMapper goodsMapper;

	@Autowired
	private TbGoodsDescMapper goodsDescMapper;

	@Autowired
	private TbItemMapper itemMapper;

	@Autowired
	private TbItemCatMapper itemCatMapper;

	@Autowired
	private TbBrandMapper brandMapper;

	@Autowired
	private TbSellerMapper sellerMapper;
	/**
	 * 查询全部
	 */
	@Override
	public List<TbGoods> findAll() {
		return goodsMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbGoods> page=   (Page<TbGoods>) goodsMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	//增加 修改 SKU列表需要调度的方法
	private void setItemValue(TbItem item,Goods goods,TbGoods tbGoods){
		//商品分类
		item.setCategoryid(tbGoods.getCategory3Id());//

		item.setCreateTime(new Date());
		item.setUpdateTime(new Date());

		item.setGoodsId(tbGoods.getId());
		item.setSellerId(tbGoods.getSellerId());
		//分类名称
		TbItemCat itemCat = itemCatMapper.selectByPrimaryKey(tbGoods.getCategory3Id());
		item.setCategory(itemCat.getName());

		//品牌
		TbBrand brand = brandMapper.selectByPrimaryKey(tbGoods.getBrandId());
		item.setBrand(brand.getName());

		//商铺名称
		TbSeller seller = sellerMapper.selectByPrimaryKey(tbGoods.getSellerId());
		item.setSeller(seller.getNickName());

		//增加图片
		List<Map> imageList = JSON.parseArray(goods.getGoodsDesc().getItemImages(), Map.class);
		if (imageList.size() != 0) {
			item.setImage((String) imageList.get(0).get("url"));
		}
	}

	//保存数据
	private void SaveIteamList(TbGoods tbGoods,Goods goods){
		if ("1".equals(tbGoods.getIsEnableSpec())) {

			for (TbItem item : goods.getItemList()) {
				//构建一个title字段 SPU+规格选项值
				String title = tbGoods.getGoodsName();//spu名称
				Map<String, Object> map = JSON.parseObject(item.getSpec());
				for (String key : map.keySet()) {
					title += " " + map.get(key);
				}
				item.setTitle(title);

				setItemValue(item,goods,tbGoods);


				itemMapper.insert(item);
			}
		}else{
			TbItem item=new TbItem();
			item.setTitle(tbGoods.getGoodsName());
			item.setPrice(tbGoods.getPrice());
			item.setNum(9999);
			item.setStatus("1");
			item.setIsDefault("1");
			item.setSpec("{}");
			setItemValue(item,goods,tbGoods);

			itemMapper.insert(item);
		}
	}
	/**
	 * 增加
	 */
	@Override
	public void add(Goods goods) {
		TbGoods tbGoods=goods.getGoods();
		tbGoods.setIsMarketable("0");
		tbGoods.setAuditStatus("0");
		goodsMapper.insert(tbGoods);

		goods.getGoodsDesc().setGoodsId(tbGoods.getId());
		goodsDescMapper.insert(goods.getGoodsDesc());

		SaveIteamList(tbGoods,goods);

	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(Goods goods){
		//更新基本表数据
		TbGoods tbGoods=goods.getGoods();
		goodsMapper.updateByPrimaryKey(goods.getGoods());

		//更新扩展表数据
		goodsDescMapper.updateByPrimaryKey(goods.getGoodsDesc());

		//删除原有的SKU列表
		TbItemExample example=new TbItemExample();
		TbItemExample.Criteria criteria=example.createCriteria();
		criteria.andGoodsIdEqualTo(goods.getGoods().getId());
		itemMapper.deleteByExample(example);

		//插入新的SKU列表
		SaveIteamList(tbGoods,goods);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public Goods findOne(Long id){
		Goods goods=new Goods();
		TbGoods tbGoods=goodsMapper.selectByPrimaryKey(id);
		goods.setGoods(tbGoods);

		TbGoodsDesc tbGoodsDesc=goodsDescMapper.selectByPrimaryKey(id);
		goods.setGoodsDesc(tbGoodsDesc);

		TbItemExample example=new TbItemExample();
		TbItemExample.Criteria criteria=example.createCriteria();
		criteria.andGoodsIdEqualTo(id);
		List<TbItem> itemList=itemMapper.selectByExample(example);
		goods.setItemList(itemList);
		return goods;
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			TbGoodsDescExample example=new TbGoodsDescExample();
			TbGoodsDescExample.Criteria criteria=example.createCriteria();
			criteria.andGoodsIdEqualTo(id);
			goodsDescMapper.deleteByExample(example);

			TbItemExample example2=new TbItemExample();
			TbItemExample.Criteria criteria1=example2.createCriteria();
			criteria1.andGoodsIdEqualTo(id);
			itemMapper.deleteByExample(example2);

			goodsMapper.deleteByPrimaryKey(id);

		}		
	}
	
	
		@Override
	public PageResult findPage(TbGoods goods, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbGoodsExample example=new TbGoodsExample();
		TbGoodsExample.Criteria criteria = example.createCriteria();
		criteria.andIsDeleteIsNull();
		if(goods!=null){			
			if(goods.getSellerId()!=null && goods.getSellerId().length()>0){
				//criteria.andSellerIdLike("%"+goods.getSellerId()+"%");
				criteria.andSellerIdEqualTo(goods.getSellerId());
			}
			if(goods.getGoodsName()!=null && goods.getGoodsName().length()>0){
				criteria.andGoodsNameLike("%"+goods.getGoodsName()+"%");
			}
			if(goods.getAuditStatus()!=null && goods.getAuditStatus().length()>0){
				criteria.andAuditStatusLike("%"+goods.getAuditStatus()+"%");
			}
			if(goods.getIsMarketable()!=null && goods.getIsMarketable().length()>0){
				criteria.andIsMarketableLike("%"+goods.getIsMarketable()+"%");
			}
			if(goods.getCaption()!=null && goods.getCaption().length()>0){
				criteria.andCaptionLike("%"+goods.getCaption()+"%");
			}
			if(goods.getSmallPic()!=null && goods.getSmallPic().length()>0){
				criteria.andSmallPicLike("%"+goods.getSmallPic()+"%");
			}
			if(goods.getIsEnableSpec()!=null && goods.getIsEnableSpec().length()>0){
				criteria.andIsEnableSpecLike("%"+goods.getIsEnableSpec()+"%");
			}
			if(goods.getIsDelete()!=null && goods.getIsDelete().length()>0){
				criteria.andIsDeleteLike("%"+goods.getIsDelete()+"%");
			}
	
		}
		
		Page<TbGoods> page= (Page<TbGoods>)goodsMapper.selectByExample(example);		
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Override
	public void UpdateStatus(Long[] ids, String status) {
		for (Long id:ids){
			TbGoods goods=goodsMapper.selectByPrimaryKey(id);
			goods.setAuditStatus(status);
			goodsMapper.updateByPrimaryKey(goods);



		}
	}

	public void deleteManager(Long[] ids){
		for (Long id:ids){
			TbGoods goods=goodsMapper.selectByPrimaryKey(id);
			goods.setIsDelete("1");
			goodsMapper.updateByPrimaryKey(goods);
		}
	}

	@Override
	public void UpdateMarkStatus(Long id, String status) {
		TbGoods goods=goodsMapper.selectByPrimaryKey(id);
		goods.setIsMarketable(status);

		TbItemExample example=new TbItemExample();
		TbItemExample.Criteria criteria=example.createCriteria();
		criteria.andGoodsIdEqualTo(id);
		List<TbItem> list=itemMapper.selectByExample(example);
		for (TbItem l:list){
			l.setStatus(status);
			itemMapper.updateByPrimaryKey(l);
		}

		goodsMapper.updateByPrimaryKey(goods);
	}

	//根据SPU的ID集合查询SKU列表 然后动态更新Solr
	public List<TbItem> findItemListByGoodsIdListAndStatus(Long goodsId,String status){

			TbItemExample example=new TbItemExample();
			TbItemExample.Criteria criteria=example.createCriteria();
			criteria.andStatusEqualTo(status);//状态
			criteria.andGoodsIdEqualTo(goodsId);//指定条件 SPUID集合
		List<TbItem> itemList = itemMapper.selectByExample(example);
		for (TbItem item:itemList){
			//[] 方括号 表示集合 可以用parseArray  { }开始表示对象 得用Object
			Map map= JSON.parseObject(item.getSpec(),Map.class);
			item.setSpecMap(map);
		}

		return itemList;

	}

}
