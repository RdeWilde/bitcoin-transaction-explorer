package com.yoghurt.crypto.transactions.server.util.json;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;

import com.yoghurt.crypto.transactions.shared.domain.BlockInformation;
import com.yoghurt.crypto.transactions.shared.util.ArrayUtil;
import com.yoghurt.crypto.transactions.shared.util.NumberEncodeUtil;

public class JSONRPCParser {
  private static final String ZERO_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

  private JSONRPCParser() {}

  public static String getResultString(final InputStream jsonData) throws JsonProcessingException, IOException {
    final JsonNode tree = JsonParser.mapper.readTree(jsonData);

    return tree.get("result").getTextValue();
  }

  public static String getRawBlock(final InputStream jsonData) throws JsonProcessingException, IOException, DecoderException {
    final JsonNode tree = getResultNode(jsonData);

    final StringBuilder builder = new StringBuilder();

    // Version
    builder.append(Hex.encodeHex(NumberEncodeUtil.encodeUint32(tree.get("version").getLongValue())));

    // Prev block hash (LE<>BE)
    final JsonNode prevBlockHashNode = tree.get("previousblockhash");

    // prevBlockHashNode is null for the genesis block
    byte[] prevBlockHash;
    if (prevBlockHashNode == null) {
      prevBlockHash = Hex.decodeHex(ZERO_HASH.toCharArray());
    } else {
      prevBlockHash = Hex.decodeHex(prevBlockHashNode.getTextValue().toCharArray());
    }
    ArrayUtil.reverse(prevBlockHash);
    builder.append(Hex.encodeHex(prevBlockHash));

    // Merkle root (LE<>BE)
    final byte[] merkleroot = Hex.decodeHex(tree.get("merkleroot").getTextValue().toCharArray());
    ArrayUtil.reverse(merkleroot);
    builder.append(Hex.encodeHex(merkleroot));

    // Timestamp
    builder.append(Hex.encodeHex(NumberEncodeUtil.encodeUint32(tree.get("time").getLongValue())));

    // Bits (LE<>BE)
    final byte[] bits = Hex.decodeHex(tree.get("bits").getTextValue().toCharArray());
    ArrayUtil.reverse(bits);
    builder.append(Hex.encodeHex(bits));

    // Nonce
    builder.append(Hex.encodeHex(NumberEncodeUtil.encodeUint32(tree.get("nonce").getLongValue())));

    return builder.toString();
  }

  private static JsonNode getResultNode(final InputStream jsonData) throws JsonProcessingException, IOException {
    return JsonParser.mapper.readTree(jsonData).get("result");
  }

  public static BlockInformation getBlockInformation(final InputStream jsonData) throws JsonProcessingException, IOException {
    final JsonNode tree = getResultNode(jsonData);

    final BlockInformation blockInformation = new BlockInformation();

    // Set the height
    blockInformation.setHeight(tree.get("height").getIntValue());

    // Set the next block hash, if any
    final JsonNode nextBlockHashNode = tree.get("nextblockhash");
    blockInformation.setNextBlockHash(nextBlockHashNode == null ? null : nextBlockHashNode.getTextValue());

    // Set the number of confirmations
    blockInformation.setNumConfirmations(tree.get("confirmations").getIntValue());

    // Set the number of transactions
    blockInformation.setNumTransactions(tree.get("tx").size());

    // Set the byte size
    blockInformation.setSize(tree.get("size").getLongValue());

    return blockInformation;
  }
}
