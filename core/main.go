package main

import (
	"flag"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"

	"github.com/metacubex/mihomo/hub/executor"
	"github.com/metacubex/mihomo/log"
)

func main() {
	configDir := flag.String("d", ".", "set configuration directory")
	configFile := flag.String("f", "config.yaml", "specify configuration file")
	flag.Parse()

	fullPath := filepath.Join(*configDir, *configFile)
	log.Infoln("NekoTale Core starting with config: %s", fullPath)

	cfg, err := executor.ParseWithPath(fullPath)
	if err != nil {
		log.Fatalln("Parse config error: %s", err.Error())
	}

	executor.ApplyConfig(cfg, true)
	log.Infoln("NekoTale Core started successfully on port 10808")

	sig := make(chan os.Signal, 1)
	signal.Notify(sig, syscall.SIGINT, syscall.SIGTERM)
	<-sig

	log.Infoln("NekoTale Core stopping")
}
