package com.minimarket.service.impl;

import com.minimarket.controller.UserController;
import com.minimarket.dao.transactionRecordDao;
import com.minimarket.model.ReturnMsg;
import com.minimarket.model.transactionRecord;
import com.minimarket.service.transactionRecordService;
import com.minimarket.utils.AddressUtil;
import com.minimarket.utils.Initial;
import com.minimarket.utils.returnMsgUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.minimarket.utils.AddressUtil.getAddress;

/**
 * @author ronjod
 * @create 2019-10-07 14:54
 */
@Service("transactionRecordService")
public class transactionRecordServiceImpl implements transactionRecordService {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final String ADDRESS = "/Users/qishuo/Downloads/block/";
    //private static final String ADDRESS = "E:\\wallet\\";
    private static final String CONTRACTADDRESS = "0x348f4392386ca41578018301c27fe960e4c35501";

    @Resource
    private transactionRecordDao transactionRecordDao;

    private static boolean doTransaction(String from,
                                         String to,
                                         BigInteger value,
                                         String password) throws ExecutionException, InterruptedException { //存在区块信息的时候再执行插入，但是这样貌似会有延迟
        if(Integer.valueOf(getBalance2(getAddress(from)))<Integer.valueOf(value.toString()))
            return false;

        Web3j web3j = Initial.getWeb3j();
        //加载转账所需的凭证，用私钥



        StringBuffer sb = new StringBuffer(ADDRESS).append(from);
//        String sb = ADDRESS+from;
        //这里改成用bip的方法创建凭证试一下
//        String privateKey = getPriKey(getCredentialByToken(password, "profit chicken weapon economy axis poem pony labor crop morning calm indicate"));
        Credentials credential = getCredential(password, sb.toString());
                String privateKey = getPriKey(credential);
        //私钥这里直接通过文件位置获取了
        Credentials credentials = Credentials.create(privateKey);
        //获取nonce，交易笔数
        BigInteger nonce = getNonce(getAddress(from));
        //get gasPrice
        BigInteger gasPrice = BigInteger.valueOf(22_000_000_000L);
        BigInteger gasLimit = BigInteger.valueOf(4_300_000);

        //创建RawTransaction交易对象
        Function function = new Function("transfer",
                Arrays.asList(new Address(getAddress(to)), new Uint256(value)),
                Arrays.asList(new TypeReference<Type>() {
                }));

        String encodedFunction = FunctionEncoder.encode(function);
//创建离线签名
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce,
                gasPrice,
                gasLimit,
                CONTRACTADDRESS, encodedFunction);

        //签名Transaction，这里要对交易做签名
        byte[] signMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signMessage);
        //发送交易
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        logger.info("11111111111111111" + ethSendTransaction.getTransactionHash());

        String temp = ethSendTransaction.getTransactionHash();
        Thread.sleep(17000l);//等待17s
        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(temp).sendAsync().get();
        if (ethTransaction.getResult().getBlockHash() != null) {
            return true;
        } else {
            return false;
        }
    }

    private static String getPriKey(Credentials credentials) {
        return credentials.getEcKeyPair().getPrivateKey().toString(16);
    }

    //获取公钥
    private static String getPubKey(Credentials credentials) {
        return credentials.getEcKeyPair().getPublicKey().toString(16);
    }

    //获取本地的钱包地址并建立连接
    private static Credentials getCredential(String password, String walletFileName) {
        Credentials credentials = null;
        try {
            credentials = WalletUtils.loadCredentials(password, walletFileName);
            System.out.println("1");
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("文件不存在");
        } catch (CipherException e) {
            e.printStackTrace();
            logger.info("密钥错误");
        }
        return credentials;
    }
    private static Credentials getCredentialByToken(String password, String walletFileName) {
        Credentials credentials = null;
        credentials = WalletUtils.loadBip39Credentials(password, walletFileName);
        return credentials;
    }

    private static BigInteger getNonce(String from) throws ExecutionException, InterruptedException {
        Web3j web3j = Initial.getWeb3j();
        EthGetTransactionCount transactionCount = web3j.ethGetTransactionCount(from, DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = transactionCount.getTransactionCount();
        return nonce;
    }
    //获取私钥

    //以太币的交易
    public  void transferEth(String walletAddress) throws ExecutionException, InterruptedException {
        //设置需要的矿工费
        BigInteger GAS_PRICE = BigInteger.valueOf(22_000_000_000L);
        BigInteger GAS_LIMIT = BigInteger.valueOf(4_300_000);

        //调用的是kovan测试环境，这里使用的是infura这个客户端
        Web3j web3j = Initial.getWeb3j(); //转账人账户地址
        String ownAddress = "UTC--2019-10-04T12-41-16.567558100Z--42c35e4a5232ddc3179e8bcd5125f203d0d1a7ed";
        //被转人账户地址
        String toAddress = getAddress(walletAddress);
        //转账人私钥
        //Credentials credentials = Credentials.create("40d9c10db9a6c5526c8e85bcba5e7260ccfd58c1219f91e60cba50b1e48eb1fe");
        Credentials credentials = getCredential("licheng",ADDRESS+ ownAddress);

        //getNonce（这里的Nonce我也不是很明白，大概是交易的笔数吧）
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                getAddress(ownAddress),DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        //创建交易，这里是转0.5个以太币
        BigInteger value = Convert.toWei("0.5", Convert.Unit.ETHER).toBigInteger();
        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                nonce, GAS_PRICE, GAS_LIMIT, toAddress, value);

        //签名Transaction，这里要对交易做签名
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        //发送交易
        EthSendTransaction ethSendTransaction =
                web3j.ethSendRawTransaction(hexValue).sendAsync().get();
        String transactionHash = ethSendTransaction.getTransactionHash();

        //获得到transactionHash后就可以到以太坊的网站上查询这笔交易的状态了
        System.out.println(transactionHash);

    }



    @Override
    public List<String> createAccount(String password) {
        String walletFilePath = ADDRESS;//根据到时候的服务器部署
        String walletFileName = null;
        String wallet = null;
        try {
//            wallet = WalletUtils.generateBip39Wallet(password, new File(walletFilePath));
wallet=WalletUtils.generateNewWalletFile(password, new File(walletFilePath));

            walletFileName = wallet;
        } catch (CipherException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        //String token = wallet.getMnemonic();
        List<String> list = new ArrayList<>(2);
        list.add(walletFileName);
//        list.add(token);
//这里应该返回地址还是返回文件名
        return list;//返回完整文件名
    }

    //获取余额
    @Override
    public String getBalance(String address) throws ExecutionException, InterruptedException {
        Function function = new Function("balanceOf",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<Address>() {
                }));

        Web3j web3j = Initial.getWeb3j();
        String encode = FunctionEncoder.encode(function);
        //logger.info( "getERC20Balance encode : " + encode);
        Transaction ethCallTransaction = Transaction.createEthCallTransaction(address, CONTRACTADDRESS, encode);
        EthCall ethCall = web3j.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).sendAsync().get();
        String value = ethCall.getResult();
        //logger.info( "getERC20Balance balance : " +  value.substring(2));

        value = new BigInteger(value.substring(2), 16).toString();
        return value;
    }
    public static String getBalance2(String address) throws ExecutionException, InterruptedException {
        Function function = new Function("balanceOf",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<Address>() {
                }));

        Web3j web3j = Initial.getWeb3j();
        String encode = FunctionEncoder.encode(function);
        //logger.info( "getERC20Balance encode : " + encode);
        Transaction ethCallTransaction = Transaction.createEthCallTransaction(address, CONTRACTADDRESS, encode);
        EthCall ethCall = web3j.ethCall(ethCallTransaction, DefaultBlockParameterName.LATEST).sendAsync().get();
        String value = ethCall.getResult();
        //logger.info( "getERC20Balance balance : " +  value.substring(2));

        value = new BigInteger(value.substring(2), 16).toString();
        return value;
    }
    @Override
    public ReturnMsg getTransactionRecord(transactionRecord address) {
        List<transactionRecord> transactionRecords = transactionRecordDao.selectTransRecord(address);
        if (transactionRecords != null) {
            return returnMsgUtil.quickReturnMsg(transactionRecords, String.valueOf(transactionRecords.size()), true);
        } else {
            return returnMsgUtil.quickReturnMsg("数据不存在", false);
        }
    }

    //对合约代币进行交易
    @Override
    public ReturnMsg insertTransactionRecord(transactionRecord temp) throws ExecutionException, InterruptedException { //存在区块信息的时候再执行插入，但是这样貌似会有延迟

        String from = temp.getTransOutUser();
        String to = temp.getTransInUser();
        BigInteger value = temp.getAmount();
        String password = transactionRecordDao.getPasswordByAddress(temp.getTransOutUser());//用比较坑爹办法获取密码
        //String password = "licheng";//用比较坑爹办法获取密码
        if (doTransaction(from, to, value, password)) {
           // transactionRecordDao.insertTransRecord(temp);
            return returnMsgUtil.quickReturnMsg("交易成功", true);
        } else {
            return returnMsgUtil.quickReturnMsg("交易失败，数据不插入", false);
        }
    }
    public void freeHelp(String to) throws ExecutionException, InterruptedException {
        String from = "UTC--2019-10-04T12-41-16.567558100Z--42c35e4a5232ddc3179e8bcd5125f203d0d1a7ed";
        //String to = temp.getTransInUser();
        BigInteger value = BigInteger.valueOf(100000l);
        String password = "licheng";//用比较坑爹办法获取密码
        if (doTransaction(from, to, value, password)) {
            //transactionRecordDao.insertTransRecord(temp);
            System.out.println("新用户注册，活动期间送你咱的10块钱吧");
        }
    }
@Test
    public void temp() throws ExecutionException, InterruptedException {
        String from="";
        String  to="";

    System.out.println(doTransaction(from,to,BigInteger.valueOf(10000),"licheng"));
}
}
