package org.orisland;

import Tool.HttpClient;
import Tool.MiraiTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.tools.javac.comp.Todo;
import net.mamoe.mirai.console.command.CommandSenderOnMessage;
import net.mamoe.mirai.console.command.java.JCompositeCommand;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.annotations.NotNull;
import org.orisland.bean.sreachImg.header;
import org.orisland.bean.pmodel.JsonRootBean;
import org.orisland.bean.sreachImg.result;

import java.io.IOException;
import java.net.URL;

/**
 * @Author: zhaolong
 * @Time: 00:05
 * @Date: 2021年07月30日 00:05
 **/
public class Mycommand extends JCompositeCommand {
    public static final Mycommand INSTANCE = new Mycommand();
    public static final ObjectMapper mapper =  new ObjectMapper();
    public static long callBack = 30000L;

    static {
        if (callBack == 0 || callBack >= 120000L){
            callBack = 130000L;
            System.out.println("初始化不撤回消息。");
        }
    }

    private Mycommand(){
        super(Plugin.INSTANCE, "pic", new String[]{"p"}, Plugin.INSTANCE.getParentPermission());
    }


//    @SubCommand("撤回")
    @SubCommand
    @Description("尝试骗过上帝。")
    public void setBack(CommandSenderOnMessage sender, long number){
        if (number <= 0 || number >= 12000L){
            callBack = 130000L;
        }else {
            callBack = number * 1000;
        }
        sender.sendMessage("欺骗上帝的撤回已设置为："+callBack / 1000 +"秒！");
    }

//    @SubCommand("设置撤回")
    @SubCommand
    @Description("告诉我当前撤回时间。")
    public void showBack(CommandSenderOnMessage sender){
        sender.sendMessage("callBack："+callBack / 1000 +"秒！");
    }

    @SubCommand
//    @SubCommand("获取")
    @Description("pid提取：p pid 作品id，即可提取该id的图片，此指令同样支持bid")
    public void pid(CommandSenderOnMessage sender, String mes) throws IOException {
        long pid = 0L;
        try {
            if (mes.isEmpty())
                return;
            pid = Long.valueOf(mes);
        }catch (Exception e){
            sender.sendMessage("输入信息不符合要求，禁止空格和其他符号。");
            return;
        }

        sender.sendMessage("bot收到了提取调用！");
        JsonNode jsonNode = HttpClient.apiGetByJson("https://pximg.rainchan.win/imginfo", "img_id="+pid);
        JsonRootBean res = mapper.readValue(jsonNode.toString(), JsonRootBean.class);
        MessageChain chin = null;
        Image image = null;
        String ps = null;

        try {
            if (jsonNode.toString().contains("R-18")){
                System.out.println("R18 禁止");
            }else {
                image = MiraiTool.getImg(sender, HttpClient.pixyProxy(res.getUrls().getRegular()));
            }
        }catch (Exception e){
            sender.sendMessage(e.getMessage());
        }
        chin = new MessageChainBuilder()
                .append(new At(sender.getUser().getId()))
                .append("我帮你找到了这张图!")
                .append("\r")
                .append((image!=null ? image : new PlainText("warn:R18禁止发送.请手动使用链接.")))
                .append("\r")
                .append("标题:" + res.getTitle())
                .append("\r")
                .append("id:" + pid)
                .append("\r")
                .append("作者:" + res.getUserName())
                .append("\r")
                .append("url:" + HttpClient.pixyProxy(res.getUrls().getOriginal()))
                .append("\r")
                .append("你可以继续回复该信息并注明\"p bid\"以获取高清原图！")
                .append("\r")
                .append("图片过大可能需要非常久的下载时间")
                .build();

        sender.sendMessage(chin).recallIn(callBack);
    }

    @SubCommand
//    @SubCommand("提取")
    @Description("bid用于查询，在其他两个指令执行的时候，回复bot的查询信息，并删除@bot字段，补上p bid即可取图")
    public void bid(CommandSenderOnMessage sender) throws IOException {
        QuoteReply reply = sender.getFromEvent().getMessage().get(QuoteReply.Key);
        if (reply == null){
            System.out.println("跳出.");
            return;
        }

        MessageChain chain = reply.getSource().getOriginalMessage();

        sender.sendMessage("bot收到了提取图片的调用！\rbot需要检测服务器是否可用!\r该过程可能根据图片的大小需要1秒到1天不等！");
        //反代理默认
//        检测服务器是否可用
        String backUrl = "https://pixiv.cat/";
        boolean flag = true;
        URL url = new URL("https://pximg.rainchan.win/");
        try {
            if (url.openConnection().getContent() != null){
                System.out.println("mirai服务器可用，替换代理服务器!");
                backUrl = "https://pximg.rainchan.win/";
                flag = false;
            }
        }catch (Exception e){
            System.out.println("出错，使用默认服务器.");
        }

        long pid = 0L;
        Image image = null;
        MessageChain back = null;
        try {
            if (chain.contentToString().contains("id") && !chain.contentToString().contains("R18")){
                pid = Long.parseLong(chain.contentToString().split("id:")[1].split(" 作者:")[0]);
                image = MiraiTool.getImg(sender, flag ? backUrl + pid + ".jpg" : backUrl + "img?img_id=" + pid);
                chain = new MessageChainBuilder()
                        .append(new At(sender.getUser().getId()))
                        .append("\n")
                        .append("pid:" + pid)
                        .append("\n")
                        .append("bot帮你下载了原图！")
                        .append("\n")
                        .append((image!=null ? image : new PlainText("R18禁止发送.\n")))
                        .build();
            }else {
                if (chain.contentToString().contains("R18")){
                    chain = new MessageChainBuilder()
                            .append(new At(sender.getUser().getId()))
                            .append("R18禁止提取图片。")
                            .build();
                }else {
                    chain = new MessageChainBuilder()
                            .append(new At(sender.getUser().getId()))
                            .append("哎呀，回复的链接没有pid项哦！")
                            .build();
                }

            }
        }catch (Exception e){
            e.printStackTrace();
            chain = new MessageChainBuilder()
                    .append("这个信息不符合规范。")
                    .build();
        }
        sender.sendMessage(chain).recallIn(callBack);


    }

    @SubCommand
//    @SubCommand("搜索")
    @Description("simg输入样例:p simg 图片 <-注意图片和simg有个空格，不能直接放图！这个指令可以搜图，暂时不支持异步")
    public void simg(CommandSenderOnMessage sender) throws IOException {
        Image mes = null;
        //TODO 懒货
        QuoteReply reply = null;
        if (sender.getFromEvent().getMessage().get(Image.Key) != null){
            mes = sender.getFromEvent().getMessage().get(Image.Key);
        }else {
            //error
            mes = sender.getFromEvent().getMessage().get(QuoteReply.Key).getSource().getOriginalMessage().get(Image.Key);
        }

        if (mes == null){
            sender.sendMessage("未找到图片，跳出。");
            return;
        }

        sender.sendMessage("收到了图片查询请求，正在尝试查找。");

        System.out.println("开始执行查询" + mes.getImageId());
        System.out.println("开始执行查询" + Image.queryUrl(mes));

        JsonNode jsonNode = HttpClient.apiGetByJson("https://saucenao.com/search.php", "api_key=b68b3740d278e39c0d16c496f75504a4b56a45eb",
                "output_type=2", "numres=1", "url=" + net.mamoe.mirai.message.data.Image.queryUrl(mes));
        //结果集映射
        result res = mapper.readValue(jsonNode.get("results").get(0).toString(), result.class);
        MessageChain chain = null;
        try {
            double precent = Double.parseDouble(res.getHeader().getSimilarity());
            String url = res.getData().getExt_urls().get(0).toString().split("\"")[1];
            String title = res.getData().getTitle();
            String pixiv_id = res.getData().getPixiv_id();
            String member_name = res.getData().getMember_name();
            String member_id = res.getData().getMember_id();
            String thumbnail = res.getHeader().getThumbnail();
            ExternalResource ex = null;
            Image image = null;
            if (HttpClient.apiGetByJson("https://pximg.rainchan.win/imginfo", "img_id="+pixiv_id).toString().contains("R-18")){
                System.out.println("发现R18tag");
            }else {
                image = MiraiTool.getImg(sender, thumbnail);
            }

            if (jsonNode.toString().contains("twitter")){
                System.out.println("推特图");
                chain = new MessageChainBuilder()
                        .append(new At(sender.getUser().getId()))
                        .append("\r")
                        .append((image!=null ? image : new PlainText("R18禁止发送.\n")))
                        .append("\r")
                        .append("非pixiv图片")
                        .append("\r")
                        .append("相关性:" + precent + "%")
                        .append("\r")
                        .append("来源:" + url)
                        .build();
            }else if (jsonNode.toString().contains("pixiv_id")){
                System.out.println("p站图");
                try {
                    chain = new MessageChainBuilder()
                            .append(new At(sender.getUser().getId()))
                            .append("\r")
                            .append((image!=null ? image : new PlainText("R18禁止发送.\n")))
                            .append("\r")
                            .append("相关性:" + precent + "%")
                            .append("\r")
                            .append("标题:" + title)
                            .append("\r")
                            .append("id:" + pixiv_id)
                            .append("\r")
                            .append("作者:" + member_name)
                            .append("\r")
                            .append("作者id:" + member_id)
                            .append("\r")
                            .append("来源:" + url)
                            .append("\r")
                            .append("反代:" + res.getHeader().getIndex_name().split(" - ")[1].split("\"")[0])
                            .build();
                }catch (Exception e){
                    System.out.println("p站报错图");
                    chain = new MessageChainBuilder()
                            .append(new At(sender.getUser().getId()))
                            .append("\r")
                            .append((image!=null ? image : new PlainText("R18禁止发送.")))
                            .append("\r")
                            .append("相关性:" + precent + "%")
                            .append("\r")
                            .append("标题:" + title)
                            .append("\r")
                            .append("id:" + pixiv_id)
                            .append("\r")
                            .append("来源:" + url)
                            .build();
                }
            }else {
                System.out.println("未知图");
                chain = new MessageChainBuilder()
                        .append(new At(sender.getUser().getId()))
                        .append("\r")
                        .append((image!=null ? image : new PlainText("R18禁止发送.\n")))
                        .append("\r")
                        .append("非pixiv图片")
                        .append("\r")
                        .append("相关性:" + precent + "%")
                        .append("\r")
                        .append("来源:" + url)
                        .build();
            }
        }catch (Exception e){
            System.out.println("出错未知图");

            header header = mapper.readValue(jsonNode.get("results").get(0).get("header").toString(), header.class);
            ExternalResource ex = null;
            Image image = null;
            try {
                image = MiraiTool.getImg(sender, header.getThumbnail());
            }catch (Exception e1){
                if (image == null){
                    chain = new MessageChainBuilder()
                            .append(new At(sender.getUser().getId()))
                            .append("\n")
                            .append("引发了万恶的NullPointerException!")
                            .append("\n")
                            .append("图片下载失败")
                            .append("\n")
                            .append("相似度:" + header.getSimilarity() + "%")
                            .append("\n")
                            .append("↓的万恶文字引发了异常bot已经懒得处理了!")
                            .append("\n")
                            .append(jsonNode.get("results").get(0).get("data").toString())
                            .build();
                }else {
                    chain = new MessageChainBuilder()
                            .append(new At(sender.getUser().getId()))
                            .append("\n")
                            .append("引发了万恶的NullPointerException!")
                            .append("\n")
                            .append((image!=null ? image : new PlainText("R18禁止发送.\n")))
                            .append("\n")
                            .append("相似度:" + header.getSimilarity() + "%")
                            .append("\n")
                            .append("↓的万恶文字引发了异常bot已经懒得处理了!")
                            .append("\n")
                            .append(jsonNode.get("results").get(0).get("data").toString())
                            .build();
                }
            }
        }
        sender.sendMessage(chain).recallIn(callBack);
    }
}
