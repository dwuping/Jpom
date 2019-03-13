package cn.keepbx.jpom.service.system;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.jpom.common.BaseDataService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.odiszapc.nginxparser.NgxBlock;
import com.github.odiszapc.nginxparser.NgxConfig;
import com.github.odiszapc.nginxparser.NgxEntry;
import com.github.odiszapc.nginxparser.NgxParam;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * @author Arno
 */
@Service
public class NgxService extends BaseDataService {

    @Resource
    private SystemService systemService;

    public JSONArray list() {
        JSONArray ngxDirectory = systemService.getNgxDirectory();
        JSONArray array = new JSONArray();
        for (Object o : ngxDirectory) {
            String parentPath = o.toString();
            //获得指定目录下所有文件
            List<String> list = null;
            try {
                list = FileUtil.listFileNames(parentPath);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error(e.getMessage(), e);
            }
            if (list == null || list.size() <= 0) {
                continue;
            }
            for (String s : list) {
                if (!s.endsWith(".conf")) {
                    continue;
                }
                String path = parentPath + s;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("path", path);
                jsonObject.put("name", s);
                jsonObject.put("whitePath", parentPath);
                try {
                    NgxConfig config = NgxConfig.read(path);
                    NgxBlock http = config.findBlock("http");
                    String severName;
                    if (null != http) {
                        List<NgxEntry> server = http.findAll(NgxBlock.class, "server");
                        severName = findSeverName(server);
                        if (StrUtil.isNotEmpty(severName)) {
                            jsonObject.put("domain", severName);
                            array.add(jsonObject);
                            continue;
                        }
                    }
                    List<NgxEntry> server = config.findAll(NgxBlock.class, "server");
                    severName = findSeverName(server);
                    jsonObject.put("domain", severName);
                } catch (IOException e) {
                    DefaultSystemLog.ERROR().error(e.getMessage(), e);
                }
                array.add(jsonObject);
            }
        }
        return array;
    }


    /**
     * 获取域名
     *
     * @param server server块
     * @return 域名
     */
    private String findSeverName(List<NgxEntry> server) {
        if (null == server || server.size() <= 0) {
            return null;
        }
        for (NgxEntry ngxEntry : server) {
            if (null != ngxEntry) {
                NgxBlock ngxBlock = (NgxBlock) ngxEntry;
                NgxParam serverName = ngxBlock.findParam("server_name");
                if (null != serverName) {
                    return serverName.getValue();
                }
            }
        }
        return null;
    }


}