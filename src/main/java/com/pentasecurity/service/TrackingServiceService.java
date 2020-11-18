package com.pentasecurity.service;

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
	
	
	
	public List<History> searchForDataid(String dataId) {
		//List<String> ret = new ArrayList<>();
		List<History> ret = historyRepository.findByDataId(dataId);
		tree(ret, getMasterTable(dataId));
		
		return ret;
	}
	
	public String getTree(String dataId) {
		List<History> ret = historyRepository.findByDataId(dataId);
		return tree(ret, getMasterTable(dataId));
	}
	
	public String getTreeForce(String dataId) {
		List<History> ret = historyRepository.findByDataId(dataId);
		return treeForce(ret);
	}
	
	public List<History> searchAll() {
		//List<String> ret = new ArrayList<>();
		List<History> ret = historyRepository.findAll();
		
		return ret;
	}
	
	public String encryption(String content) {
		String ret = "";
		
		ret = encrypt(content, "SHA-256");
		
		System.out.println(ret);
		
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
	
	public MasterDto getMasterTable(String dataId) {
		Master master = masterRepository.findById(dataId).orElse(null);
		
		MasterDto masterDto = null;
		if(master != null) { 
			masterDto = new MasterDto(master);
		}
		return masterDto;
	}
	
	public List<Code> getDataFormatList() {
		
		return codeRepository.findAll();
	}
	
	public List<HistoryDto> conditionalSearch(ConditionSearchDto condition) {
				
		List<History> history = historyRepository.findByConditional(condition);
		
		treeForce(history);
		
		List<HistoryDto> historyDto = new ArrayList<>();
		
		for(History h : history) {
			historyDto.add(new HistoryDto(h));
		}
		
		return historyDto;	
	}
	
	public String makeTreeForce(ConditionSearchDto condition) {
		String ret = null;
		List<History> history = historyRepository.findByConditional(condition);
		
		ret = treeForce(history);
		
		
		return ret;
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
				//break;
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
		//firstObj.put("devicetype", "device");
		//firstObj.put("devicetype", );
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

		System.out.println(treeNode.get(firstNode.getFromId()).toString());
		System.out.println(treeNode.get(firstNode.getFromId()).toJSONString());
		ret = treeNode.get(firstNode.getFromId()).toJSONString();
		
		return ret;
	}

	
	/*
	@SuppressWarnings("unchecked")
	
	private String treeForce(List<History> history, MasterDto masterDto) { 
		
		String ret ="";
				
		JSONObject cloud = new JSONObject();
		
		String treeOrigin = tree(history, masterDto);
		JSONParser parser = new JSONParser();
		JSONObject tree = null;
		try {
			tree = (JSONObject)parser.parse(treeOrigin);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(tree == null) {
			ret = "error";
		}
		else {
			
			JSONArray child = (JSONArray)tree.get("children");
			JSONArray child2 = (JSONArray)(((JSONObject)child.get(0)).get("children"));
			
			child2.add(cloud);
			//TODO: devicetype, timestamp 채울것
			cloud.put("devicetype", "Central Cloud");
			cloud.put("timestamp", (String)((JSONObject)child.get(0)).get("timestamp"));
			cloud.put("actiontype", "cloud");
			cloud.put("devicetype", "Central Cloud");
			cloud.put("deviceid", "Central Cloud");
			
			ret= tree.toJSONString();
		}
		
		
		return ret;
	}
	
	*/
	@SuppressWarnings("unchecked")
	private String treeForce(List<History> history) {
		String ret ="";
		
		
		Map<String, NodeGraphInfo> nodeInfo = new HashMap<>();
		JSONArray nodeArray = new JSONArray();
		JSONArray linkArray = new JSONArray();
		
		Set<String> nodeName = new HashSet<>();
		
		
		Set<JSONObject> linkSet = new HashSet<>();
		for(History member : history) {
			nodeName.add(member.getFromId());
			nodeName.add(member.getToId());
		}
		
		List<String> memberList = new ArrayList<>(nodeName);
		
		int index = 0;
		
		for(String member: memberList) {
			JSONObject node = new JSONObject();
			node.put("deviceid", member);
			
			
			nodeInfo.put(member, new NodeGraphInfo(index++, node));
			nodeArray.add(node);
		}
		
		for(History h: history) {
			String toId = h.getToId();
			String fromId = h.getFromId();
			
			JSONObject link = new JSONObject();
			
			link.put("source", nodeInfo.get(fromId).getIndex());
			link.put("target", nodeInfo.get(toId).getIndex());
			
			
			nodeInfo.get(toId).getNodeInfo().put("actiontype", h.getTrace());
			nodeInfo.get(toId).getNodeInfo().put("timestamp", h.getReceivedTime());
			nodeInfo.get(toId).getNodeInfo().put("actiontype", h.getTrace());
			
			//linkArray.add(link);
			linkSet.add(link);
		}
		
		for(JSONObject link: linkSet) {
			linkArray.add(link);
		}
		
		JSONObject result = new JSONObject();
		
		result.put("nodes", nodeArray);
		result.put("links", linkArray);
		
		ret = result.toJSONString();
		
		System.out.println(ret);
		
		
		return ret;
	}
}
