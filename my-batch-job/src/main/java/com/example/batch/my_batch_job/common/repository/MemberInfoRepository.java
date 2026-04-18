package com.example.batch.my_batch_job.common.repository;

import java.util.List;

import org.apache.ibatis.cursor.Cursor;

import com.example.batch.my_batch_job.common.dto.MemberInfoDto;

public interface MemberInfoRepository {

	// 例：全件取得用メソッドを追加
	//	  List<MemberInfoDto> findAll(); 
	// もしくは Cursor を使う場合
	Cursor<MemberInfoDto> cursor();
	
	int updatePointAndStatus(MemberInfoDto memberInfo); // (2)
	  
	List<MemberInfoDto> findByStatus(String aaa);
	
	// 件数取得用
	long count();	  
}
