package com.pentasecurity.service;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.pentasecurity.dto.ConditionSearchDto;
import com.pentasecurity.dto.HistoryDto;
import com.pentasecurity.dto.MasterDto;
import com.pentasecurity.dto.NodeGraphInfo;
import com.pentasecurity.entity.Code;
import com.pentasecurity.entity.History;
import com.pentasecurity.entity.Master;
import com.pentasecurity.repository.CodeRepository;
import com.pentasecurity.repository.HistoryRepository;
import com.pentasecurity.repository.MasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrackingServiceService {
	
	private final HistoryRepository historyRepository;
	private final MasterRepository masterRepository;
	private final CodeRepository codeRepository;
	
	
	
	public List<String> downloadDataId() {
		List<String> list = new ArrayList<>();
		
		List<Master> master = masterRepository.findAll();
		
		File folder = new File("./dataid");
		if(!folder.exists()) {
			try {
				folder.mkdir();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		
		for (Master m : master) {
			
			try {
				OutputStream output = new FileOutputStream("./dataid/"+m.getDataId());
				byte[] bytes = m.getData().getBytes();
				output.write(bytes);
				output.close();
				list.add(m.getDataId());
				
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return list;
	}
	
	public List<History> searchForDataid(String dataId) {
		List<History> ret = historyRepository.findByDataId(dataId);
		return ret;
	}
	
	public String getTree(String dataId) {
		List<History> ret = historyRepository.findByDataId(dataId);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		return tree(ret, getMasterTable(dataId));
	}
	
	public String getTreeForce(String dataId) {
		List<History> ret = historyRepository.findByDataId(dataId);
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		return treeForce(ret,"","" , false);
	}
	
	public List<History> searchAll() {
		List<History> ret = historyRepository.findAll();
		
		return ret;
	}
	
	public String encryption(String content) {
		String ret = "";
		
		ret = encrypt(content, "SHA-256");
		
		return ret;
	}
	
	public List<HistoryDto> getNodeTrace(String nodeName, String dataId) {
		
		
		List<History> fromNode = historyRepository.findByFromIdAndDataId(nodeName, dataId);
		List<History> toNode = historyRepository.findByToIdAndDataId(nodeName, dataId);
		
		toNode.addAll(fromNode);
		
		List<HistoryDto> ret = new ArrayList<>();
		
		for(History h : toNode) {
			ret.add(new HistoryDto(h));
		}
		return ret;
	}
	
	public String getData(String dataId) {
		String ret = "";
		Master master = masterRepository.findById(dataId).orElse(null);
		if(master != null) { 
			ret = master.getData();
		}
		return ret;
	}
	public MasterDto getMasterTable(String dataId) {
		Master master = masterRepository.findById(dataId).orElse(null);
		
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		MasterDto masterDto = null;
		if(master != null) { 
			masterDto = new MasterDto(master);
		}
		return masterDto;
	}
	
	public List<Code> getDataFormatList() {
		
		return codeRepository.findAll();
	}
	
	public List<HistoryDto> wrappingDto(List<History> list){
		List<HistoryDto> historyDto = new ArrayList<>();
		
		for(History h : list) {
			historyDto.add(new HistoryDto(h));
		}
		
		return historyDto;	
	}
	
	public List<History> conditionalSearch(ConditionSearchDto condition) {
		
		long start = System.currentTimeMillis();
		List<History> conditionHistory = conditionalSearchHistory(condition);
		
		long end = System.currentTimeMillis();
		
		System.out.println("conditionalSearch : " + (end - start));
		
		return conditionHistory;	
	}
	
	
	public String makeTreeForce(List<History> history, String deviceId, String edgeId) {
		String ret = null;
		
		long start = System.currentTimeMillis();
		 
		ret = treeForce(history, deviceId, edgeId, true);
		
		
		long end = System.currentTimeMillis();
		
		System.out.println("makeTreeForce : " + (end - start));
		
		
		return ret;
	}
	
	private List<History> conditionalSearchHistory(ConditionSearchDto condition){
		
		
		condition.setTimeStampStart("2020-11-24 11:00:00");
		condition.setTimeStampEnd("2020-11-24 13:00:00");
		
		long start = System.currentTimeMillis();
		
		List<History> history = historyRepository.findByConditional(condition);
		
		long end = System.currentTimeMillis();
		
		System.out.println("findByConditional : " + (end - start));
		
		start = System.currentTimeMillis();
		
		List<History> conditionHistory;
		if(!condition.getEventType().equals("")) {

			conditionHistory = history.stream()
					.filter(h -> h.getTrace().equals(condition.getEventType()))
					.collect(toList());
			
		}
		else {
			
			conditionHistory = new ArrayList<>();
			
			Set<String> set = history.stream()
					.map(h -> h.getDataId())
					.collect(toSet());
		
			List<String> dataIdList = new ArrayList<>(set);
			conditionHistory.addAll(historyRepository.findAllByDataIdIn(dataIdList));
		
		}
		
		end = System.currentTimeMillis();
		System.out.println("conditionHistory.addAll(searchForDataid(s)) : " + (end - start));
		
		return conditionHistory;
	}
	
	private String encrypt(String s, String messageDigest) {
        try {
            MessageDigest md = MessageDigest.getInstance(messageDigest);
            byte[] passBytes = s.getBytes();
            md.reset();
            byte[] digested = md.digest(passBytes);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < digested.length; i++) sb.append(Integer.toString((digested[i]&0xff) + 0x100, 16).substring(1));
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }
	
	@SuppressWarnings("unchecked")
	private String tree(List<History> history, MasterDto masterDto) {
		String ret ="";
		
		Queue<History> queue = new LinkedList<>();
		
		Map<String, JSONObject> treeNode = new HashMap<>();
		Map<String, JSONArray> treeArray = new HashMap<>();
		
		Set<String> nodeName = new HashSet<>();
		History firstNode = null;
		
		for(History member : history) {
			if(member.getTrace().equals("new")){
				queue.add(member);
				firstNode = member;
			}
			nodeName.add(member.getFromId());
			nodeName.add(member.getToId());
		}
		
		List<String> memberList = new ArrayList<>(nodeName);
		
		
		for(String member: memberList) {
			JSONObject node = new JSONObject();
			JSONArray child = new JSONArray();
			
			node.put("deviceid", member);
			
			
			treeNode.put(member, node);
			treeArray.put(member, child);
			
		}
		
		JSONObject firstObj = treeNode.get(firstNode.getFromId());
		
		firstObj.put("timestamp", masterDto.getCreateTime());
		firstObj.put("actiontype", "create");
		for(History path: history) {
			String startName = path.getFromId();
			String endName = path.getToId();
			
			JSONObject nodeObject = treeNode.get(startName);
			JSONObject edgeObject = treeNode.get(endName);
			JSONArray nodeArray = treeArray.get(startName);
			
			if(!nodeObject.containsKey("devicetype"))
				nodeObject.put("devicetype", path.getFromType());
			
			edgeObject.put("timestamp", path.getReceivedTime());
			edgeObject.put("actiontype", path.getTrace());
			edgeObject.put("devicetype", path.getToType());
		
			nodeArray.add(edgeObject);
			nodeObject.put("children", nodeArray);
		}

		ret = treeNode.get(firstNode.getFromId()).toJSONString();
		
		return ret;
	}

	@SuppressWarnings("unchecked")
	private String treeForce(List<History> history, String device, String edge, boolean isCondition) {
		String ret ="";
		
		boolean isDeviceIdEmpty = false;
		boolean isEdgeIdEmpty = false;
		if(device.equals(""))
			isDeviceIdEmpty = true;
		if(edge.equals(""))
			isEdgeIdEmpty = true;
		
		Map<String, NodeGraphInfo> nodeInfo = new HashMap<>();
		JSONArray nodeArray = new JSONArray();
		JSONArray linkArray = new JSONArray();
		
		Set<String> nodeName = new HashSet<>();
		Set<String> toIdSet;
		Set<String> fromIdSet;
		
		toIdSet = history.stream()
				.filter(h -> !h.getToId().equals(""))
				.map(h -> h.getToId())
				.collect(toSet());
		
		fromIdSet = history.stream()
				.filter(h -> !h.getToId().equals(""))
				.map(h -> h.getFromId())
				.collect(toSet());
		
		nodeName.addAll(toIdSet);
		nodeName.addAll(fromIdSet);
		
		Set<JSONObject> linkSet = new HashSet<>();
		
		List<String> memberList = new ArrayList<>(nodeName);
		
		
		int index = 0;
		
		boolean hasDevice = false;
		
		
		
		for(String member: memberList) {
			JSONObject node = new JSONObject();
			if(member.equals("")) continue;
			if(member.contains("device"))
				hasDevice = true;
			node.put("deviceid", member);
			node.put("trans", 0);
			node.put("receive", 0);
			
			if(member.equals(edge))
				node.put("color","#ed7c31");  // 주황
			else if(member.equals(device))
				node.put("color","#fbc280"); // 연주황
			else 
				node.put("color","#d9d9d9"); // 회색
			
			if(isDeviceIdEmpty) {
				if(member.contains("device"))
					node.put("color","#fbc280"); // 연주황
			}
			
			if(isEdgeIdEmpty) {
				if(member.contains("edge"))
					node.put("color","#ed7c31"); //주황
			}
			
			
			node.put("isCondition", isCondition);
			nodeInfo.put(member, new NodeGraphInfo(index++, node));
			nodeArray.add(node);
		}
		
		
		int cloudIndex = index;
		JSONObject cloud = new JSONObject();
		
		if(hasDevice) {
			
			
			
			cloud.put("deviceid", "central cloud");
			cloud.put("color","#405275");
			cloud.put("trans", 0);
			cloud.put("receive", 0);
			cloud.put("isCondition", isCondition);
			nodeInfo.put("central cloud", new NodeGraphInfo(cloudIndex, cloud));
			
			nodeArray.add(cloud);
		}
		
		for(History h: history) {
			String toId = h.getToId();
			String fromId = h.getFromId();
			
			if(toId.equals("") || fromId.equals("")) continue;
			JSONObject link = new JSONObject();
			
			link.put("source", nodeInfo.get(fromId).getIndex());
			link.put("target", nodeInfo.get(toId).getIndex());
			
			if(h.getFromType().contains("device")) {
				JSONObject cloudLink = new JSONObject();
				
				cloudLink.put("source", nodeInfo.get(toId).getIndex());
				cloudLink.put("target", cloudIndex);
				cloudLink.put("color", "#ed7c31");
				
				linkSet.add(cloudLink);
			}
			
			JSONObject from = nodeInfo.get(fromId).getNodeInfo();
			JSONObject to = nodeInfo.get(toId).getNodeInfo();
			
			int receiveCount = (int)to.get("receive");
			int transCount = (int)from.get("trans");
			 
			from.put("devicetype", h.getFromType());
			from.put("trans", transCount + 1);
			
			to.put("devicetype", h.getToType());
			to.put("actiontype", h.getTrace());
			to.put("timestamp", h.getReceivedTime());
			to.put("actiontype", h.getTrace());
			to.put("receive", receiveCount + 1);
			
			
			
			if(fromId.equals(edge)) {
				to.put("color", "#0070C0"); // 파랑
				link.put("color", "#0070C0");  // 파랑
			}
			else link.put("color", "#d9d9d9");
			
			
			if(fromId.equals(device) || (isDeviceIdEmpty && h.getFromType().contains("device"))) {
				int cloudCount = (int)cloud.get("receive");
				
				cloud.put("receive", cloudCount + 1);
				link.put("color", "#ed7c31");
			}
			linkSet.add(link);
			
		}
		
		for(JSONObject link: linkSet) {
			linkArray.add(link);
		}
		
		JSONObject result = new JSONObject();
		
		result.put("nodes", nodeArray);
		result.put("links", linkArray);
		
		ret = result.toJSONString();
		
		
		return ret;
	}
}
