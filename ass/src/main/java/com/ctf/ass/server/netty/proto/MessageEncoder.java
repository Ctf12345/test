package com.ctf.ass.server.netty.proto;


import com.ctf.ass.server.component.RocketMQMessageSender;
import com.ctf.ass.server.component.SpringContextHandler;
import com.ctf.ass.server.utils.LogWrapper;
import com.ctf.ass_codec.struct.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;

//@Scope("prototype")
//@Component
public class MessageEncoder extends MessageToByteEncoder<Message> {
//    @Autowired
//    private RocketMQMessageSender rocketMQMessageSender;
    private static LogWrapper logger = LogWrapper.getLogger(MessageEncoder.class.getName());

    private int tag;

    public MessageEncoder(int tag) throws IOException {
        super();
        this.tag = tag;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, ByteBuf buf) throws Exception {
        if (message == null) {
            logger.error(ctx, "[encode ERROR] message is NULL!");
            throw new Exception("[encode ERROR] message is NULL!");
        }

        if (message.getTag() != tag) {
            logger.error(ctx, "[encode ERROR] tag:" + message.getTag() + ", expected tag:" + tag + "!");
            throw new Exception("[encode ERROR] tag:" + message.getTag() + ", expected tag:" + tag + "!");
        }

        //编码
        buf.writeInt(tag);
        int lenWriterIndex = buf.writerIndex();
        buf.writeShort(0);  //len
        buf.writeShort(message.getCmdId());
        buf.writeShort(message.getSeqNo());
        buf.writeByte(message.getProp());

        //写入body
        int len = message.getBody().toBytes(buf);
        //回写len
        int crcWriterIndex = buf.writerIndex();
        buf.writerIndex(lenWriterIndex);
        buf.writeShort(len);
        buf.writerIndex(crcWriterIndex);
        //写CRC
        buf.writeShort(Message.calcCrc16(buf, 0, crcWriterIndex));

        //===================================================================================================
        //打印log
        StringBuffer sb = new StringBuffer();
        switch (tag) {
            case Message.ToMtTag:
                sb.append("Server=>Mt ");
                break;

            case Message.ToSpTag:
                sb.append("Server=>Sp ");
                break;

            default:
                sb.append("Unknown Tag!!!");
                break;
        }
        sb.append(message);
        logger.debug(ctx, sb.toString());
        //----------------------------------------------------
        //保存操作历史
//        rocketMQMessageSender.sendOpHistory(ctx, message);
        RocketMQMessageSender rocketMQMessageSender = SpringContextHandler.getBean(RocketMQMessageSender.class);
        if(rocketMQMessageSender!=null){
            rocketMQMessageSender.sendOpHistory(ctx, message);
        }
    }
}