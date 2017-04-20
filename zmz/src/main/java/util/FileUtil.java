package util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.BusinessVO;
import model.Call;
import model.CityVO;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class FileUtil {

	/**
	 * @return 相对全路径
	 * @description 文件上传工具类
	 * @author 李长吉
	 * @version 2015年6月20日 下午4:14:22
	 * @throws Exception
	 */
	public static String fileUpload(String path, MultipartFile file, HttpServletRequest request) throws Exception {
		if (file != null && file.getSize() != 0) {
			byte[] bytes = file.getBytes();
			String uploadDir = request.getSession().getServletContext().getRealPath("/upload" + path);
			File dirPath = new File(uploadDir);
			if (!dirPath.exists()) {
				dirPath.mkdirs();
			}
			String sep = System.getProperty("file.separator");
			File uploadedFile = new File(uploadDir + sep + new java.util.Date().getTime() + file.getSize()
					+ file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));
			FileCopyUtils.copy(bytes, uploadedFile);
			return "/upload" + path + "/" + uploadedFile.getName();
		} else {
			return null;
		}
	}

	/**
	 * @description 下载工具类
	 * @author 李长吉
	 * @version 2015年6月24日 上午9:18:51
	 */
	public static ResponseEntity<byte[]> getFileStream(String url, String downName) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDispositionFormData("attachment", new String(downName.getBytes("UTF-8"), "ISO-8859-1"));
		File f = new File(url);
		return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(f), headers, HttpStatus.CREATED);
	}

	/**
	 * @description 下载
	 * @author 李长吉
	 * @version 2015年6月24日 下午12:11:37
	 */
	public static void getFileDown(HttpServletResponse response, String path, String fileName) throws Exception {
		// 设置响应头和下载保存的文件名
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
		// 打开指定文件的流信息
		java.io.OutputStream outputStream = response.getOutputStream();
		java.io.FileInputStream fileInputStream = new java.io.FileInputStream(path);
		// 写出流信息 ，在这里可以做验证
		byte[] buffer = new byte[1024];
		int i = -1;
		while ((i = fileInputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, i);
		}
		outputStream.flush();
		outputStream.close();
		fileInputStream.close();
	}

	/*
	 * 迭代复制文件夹
	 */
	public static void copy(String path, String copyPath) throws IOException {
		File filePath = new File(path);
		File cf = new File(copyPath);
		if (filePath.isDirectory()) {
			File[] list = filePath.listFiles();
			for (int i = 0; i < list.length; i++) {
				String newPath = path + File.separator + list[i].getName();
				String newCopyPath = copyPath + File.separator + list[i].getName();
				copy(newPath, newCopyPath);
			}
		} else if (filePath.isFile()) {
			if (cf.getParentFile() == null || !cf.getParentFile().exists()) {
				cf.getParentFile().mkdirs();
			}
			if (path.endsWith(".js") || path.endsWith(".html") || path.endsWith(".css")) {
				Resource res = new FileSystemResource(filePath);
				EncodedResource encRes = new EncodedResource(res, "UTF-8");
				String content = FileCopyUtils.copyToString(encRes.getReader());
				FileCopyUtils.copy(content.getBytes("utf-8"), cf);
			} else {
				FileCopyUtils.copy(filePath, cf);
			}
		} else {
			System.out.println("请输入正确的文件名或路径名");
		}
	}

	/*
	 * 生成待发送文件夹
	 */
	public static String parseGenFile(String type, String date, List<Call> callList, List<BusinessVO> businessList,
			Map<String, List<Call>> map, List<CityVO> cityList) throws Exception {
		WebApplicationContext webApplicationContext = ContextLoader.getCurrentWebApplicationContext();
		ServletContext servletContext = webApplicationContext.getServletContext();

		String path = servletContext.getRealPath("/genChart");
		File file = new File(path + "/package/统计报告.html");
		Document doc = Jsoup.parse(file, "UTF-8");
		// set date
		if ("day".equals(type)) {
			doc.select("#type option").html("日报表");
			Elements dateE = doc.select("#dayValue");
			date = date.split(" ")[0];
			dateE.val(date);
		} else if ("week".equals(type)) {
			doc.select("#type option").html("周报表");
			Elements dateE = doc.select("#dayValue");
			date = date.split(" ")[0];
			dateE.val(date);
		} else if ("month".equals(type)) {
			doc.select("#type option").html("月报表");
			Elements dateE = doc.select("#dayValue");
			date = date.substring(0, date.lastIndexOf("-"));
			dateE.val(date);
		}
		// set cities
		Elements ce = doc.select("select[name=city_id]");
		String html = "";
		for (int i = 0; i < cityList.size(); i++) {
			html += "<option value='" + cityList.get(i).getId() + "'>" + cityList.get(i).getName() + "</option>";
		}
		ce.append(html);
		// set callList
		Elements cte = doc.select("#contentTable");
		html = "";
		for (int i = 0; i < callList.size(); i++) {
			html += "<tr><td>" + (i + 1) + "</td><td>" + callList.get(i).getCatVO().getName() + "</td><td>"
					+ callList.get(i).getCatVO().getBusinessVO().getName() + "</td><td>" + callList.get(i).getCount()
					+ "</td></tr>";
		}
		cte.append(html);
		// set cats modal
		Elements cme = doc.select("#catModalBody");
		html = "";
		for (int i = 0; i < callList.size(); i++) {
			html += "<div class=\"progress\">";
			html += "<div style=\"float: left;width: 150px;\">" + callList.get(i).getCatVO().getName() + "("
					+ callList.get(i).getCatVO().getBusinessVO().getName() + ")</div> ";
			html += "<div class=\"progress-bar progress-bar-cat cat_"
					+ callList.get(i).getCatVO().getBusinessVO().getId()
					+ "\"  role=\"progressbar\" aria-valuenow=\"0\" aria-valuemin=\"0\" aria-valuemax=\"100\" >"
					+ callList.get(i).getCount();
			html += "</div><small>0%</small></div>";
		}
		cme.append(html);
		// set business modal
		Elements bme = doc.select("#businessModalBody");
		html = "";
		for (int i = 0; i < businessList.size(); i++) {
			html += "<div class=\"progress\">";
			html += "<div style=\"float: left;width: 150px;\">" + businessList.get(i).getName() + "</div>";
			html += "<div class=\"progress-bar progress-bar-business\"   role=\"progressbar\" aria-valuenow=\"0\" aria-valuemin=\"0\" aria-valuemax=\"100\" >";
			html += "<input type=\"hidden\" value=\"" + businessList.get(i).getId()
					+ "\"></div><small>0%</small></div>";
		}
		bme.append(html);
		// set data
		String input = "<input type='hidden' id='callList" + "' value='"
				+ JSON.toJSONString(callList, SerializerFeature.DisableCircularReferenceDetect) + "'>";
		doc.select("body").append(input);
		for (Map.Entry<String, List<Call>> entry : map.entrySet()) {
			String oinput = "<input type='hidden' id='callList" + entry.getKey() + "' value='"
					+ JSON.toJSONString(entry.getValue(), SerializerFeature.DisableCircularReferenceDetect) + "'>";
			doc.select("body").append(oinput);
		}

		copy(path + "/package", path + "/" + date + type);
		File f = new File(path + "/" + date + type + "/统计报告.html");
		FileCopyUtils.copy(doc.toString().getBytes("utf-8"), f);

		File zip = new File(path + "/" + date + type, date + type + ".zip");
		ZipUtil.packageZip(path + "/" + date + type, zip.getAbsolutePath());
		return zip.getAbsolutePath();
	}

	public static boolean deleteFile(String sPath) {
		boolean flag = false;
		File file = new File(sPath);
		// 路径为文件且不为空则进行删除
		if (file.isFile() && file.exists()) {
			file.delete();
			flag = true;
		}
		return flag;
	}

	/**
	 * 删除目录（文件夹）以及目录下的文件
	 * 
	 * @param sPath
	 *            被删除目录的文件路径
	 * @return 目录删除成功返回true，否则返回false
	 */
	public static boolean deleteDirectory(String sPath) {
		// 如果sPath不以文件分隔符结尾，自动添加文件分隔符
		if (!sPath.endsWith(File.separator)) {
			sPath = sPath + File.separator;
		}
		File dirFile = new File(sPath);
		// 如果dir对应的文件不存在，或者不是一个目录，则退出
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return false;
		}
		boolean flag = true;
		// 删除文件夹下的所有文件(包括子目录)
		File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			// 删除子文件
			if (files[i].isFile()) {
				flag = deleteFile(files[i].getAbsolutePath());
				if (!flag)
					break;
			} // 删除子目录
			else {
				flag = deleteDirectory(files[i].getAbsolutePath());
				if (!flag)
					break;
			}
		}
		if (!flag)
			return false;
		// 删除当前目录
		if (dirFile.delete()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 根据路径删除指定的目录或文件，无论存在与否
	 * 
	 * @param sPath
	 *            要删除的目录或文件
	 * @return 删除成功返回 true，否则返回 false。
	 */
	public static boolean DeleteFolder(String sPath) {
		boolean flag = false;
		File file = new File(sPath);
		// 判断目录或文件是否存在
		if (!file.exists()) { // 不存在返回 false
			return flag;
		} else {
			// 判断是否为文件
			if (file.isFile()) { // 为文件时调用删除文件方法
				return deleteFile(sPath);
			} else { // 为目录时调用删除目录方法
				return deleteDirectory(sPath);

			}
		}
	}
}
