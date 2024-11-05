declare module '@totalperspective/pondermatic' {
  interface Pool {
    id: string;
  }
  
  interface Local {
    engine: unknown;
  }

  export type Engine = Pool | Local;
  export type Datom = [string, string, unknown, number, boolean];

  export interface DBResult {
    'query': (query: string) => any;
    'tx-data': Datom[];
    'tempids': Record<string, string>;
    'db-uri': string;
  }

  interface TxMessage {
    '->db': {
      [key: string]: any;
    }[];
    cb?: (result: DBResult) => void;
  }

  interface UpsertMessage {
    '+>db': {
      [key: string]: any;
    }[];
    cb?: (result: DBResult) => void;
  }

  interface RetractMessage {
    '-!>db': {
      [key: string]: any;
    }[];
    cb?: (result: DBResult) => void;
  }

  type Op = 'db/add' | 'db/retract';

  interface DatomsMessage {
    '!>db': [Op, string, string, unknown][];
    cb?: (result: DBResult) => void;
  }

  export type Message = TxMessage | UpsertMessage | RetractMessage | DatomsMessage
  export type Task = { __type: 'flow' } & (() => void)
  export type State = { 'quiescent?': boolean, 'prefix': string } & { [key: string]: any }

  interface PondermaticAPI {
    createEngine(name: string, resetDb?: boolean): Engine;
    copy(engine: Engine): Engine;
    ruleset(rules: string | object): object;
    dataset(data: string | object): object;
    sh(engine: Engine, msg: Message): Promise<State>;
    cmd(msg: object): void;
    addRulesMsg(rules: object): object;
    q(engine: Engine, query: string, args: any[], callback: (result: any) => void): Task;
    qP(engine: Engine, query: string, args: any[]): Promise<any>;
    q$(engine: Engine, query: string, ...args: any[]): any;
    entity(engine: Engine, ident: string | object, callback: (entity: any) => void): Task;
    entityP(engine: Engine, ident: string | object): Promise<any>;
    basisT(engine: Engine, callback: (result: number) => void): Task;
    watchEntity(engine: Engine, ident: string | object, callback: (entity: any) => void): Task;
    hashId(obj: object): string;
    errorInfo(error: Error): object;
    portal(launcher?: string): void;
    dispose(task: Task): void;
    log(level: string | null, expr: any): void;
    unify(exprOrStr: string | object, env: object | object[]): object;
    pprint(obj: any): void;
    addTap(tap?: (x: any) => void): void;
    readString(str: string): any;
    toString(obj: any): string;
    encode(obj: any): string;
    decode(str: string): any;
    eval(str: string, toJs?: boolean, opts?: object): any;
    toJS(form: any): any;
    devtoolsFormatter: {
      header: (obj: any, config: any) => any;
      hasBody: () => boolean;
      body: (obj: any, config: any) => any;
    };
    stop(engine: Engine): void;
    watchAgents(callback: (agents: any) => void): string;
    removeAgentsWatch(id: string): void;
    isReady(engine: Engine): boolean;
    devtoolsInit(): void;
    noop: {
      rules: Message;
      engine: Message;
      db: Message;
    };
  }

  const api: PondermaticAPI;
  export default api;
}
