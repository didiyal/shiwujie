package com.swj.shiwujie.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.convert.Converter;
import cn.hutool.core.convert.ConverterRegistry;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.ObjUtil;

import java.util.Queue;

/**
 * 类型转换工具类
 */
public class ConverterUtils{


	/**
	 * Obj转换队列
	 * @param obj
	 * @return
	 */
	public static Queue<Long> ObjToQueueLong(Object obj){
		if(ObjUtil.isNull(obj)){
			return null;
		}
		return Convert.convert(new TypeReference<Queue<Long>>() {}, obj);
	}


}
