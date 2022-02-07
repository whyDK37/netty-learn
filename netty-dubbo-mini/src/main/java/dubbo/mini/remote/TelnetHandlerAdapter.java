package dubbo.mini.remote;

import dubbo.mini.common.Constants;
import dubbo.mini.common.NetURL;
import dubbo.mini.common.utils.StringUtils;
import dubbo.mini.support.ExtensionLoader;

public class TelnetHandlerAdapter extends ChannelHandlerAdapter implements TelnetHandler {

  private final ExtensionLoader<TelnetHandler> extensionLoader = ExtensionLoader
      .getExtensionLoader(TelnetHandler.class);

  @Override
  public String telnet(NetChannel channel, String message) throws RemotingException {
    String prompt = channel.getUrl()
        .getParameterAndDecoded(Constants.PROMPT_KEY, Constants.DEFAULT_PROMPT);
    boolean noprompt = message.contains("--no-prompt");
    message = message.replace("--no-prompt", "");
    StringBuilder buf = new StringBuilder();
    message = message.trim();
    String command;
    if (message.length() > 0) {
      int i = message.indexOf(' ');
      if (i > 0) {
        command = message.substring(0, i).trim();
        message = message.substring(i + 1).trim();
      } else {
        command = message;
        message = "";
      }
    } else {
      command = "";
    }
    if (command.length() > 0) {
      if (extensionLoader.hasExtension(command)) {
        if (commandEnabled(channel.getUrl(), command)) {
          try {
            String result = extensionLoader.getExtension(command).telnet(channel, message);
            if (result == null) {
              return null;
            }
            buf.append(result);
          } catch (Throwable t) {
            buf.append(t.getMessage());
          }
        } else {
          buf.append("Command: ");
          buf.append(command);
          buf.append(" disabled");
        }
      } else {
        buf.append("Unsupported command: ");
        buf.append(command);
      }
    }
    if (buf.length() > 0) {
      buf.append("\r\n");
    }
    if (!StringUtils.isEmpty(prompt) && !noprompt) {
      buf.append(prompt);
    }
    return buf.toString();
  }

  private boolean commandEnabled(NetURL url, String command) {
    String supportCommands = url.getParameter(Constants.TELNET);
    if (StringUtils.isEmpty(supportCommands)) {
      return true;
    }
    String[] commands = Constants.COMMA_SPLIT_PATTERN.split(supportCommands);
    for (String c : commands) {
      if (command.equals(c)) {
        return true;
      }
    }
    return false;
  }

}
